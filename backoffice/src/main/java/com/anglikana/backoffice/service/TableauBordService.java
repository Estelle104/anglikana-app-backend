package com.anglikana.backoffice.service;

import com.anglikana.backoffice.dto.ApercuHierarchieDTO;
import com.anglikana.backoffice.dto.ClergeAffecteDTO;
import com.anglikana.backoffice.dto.ClergeDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.NoeudHierarchiqueDTO;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.dto.RessourceDetailleeDTO;
import com.anglikana.backoffice.dto.StatistiquesEntiteDTO;
import com.anglikana.backoffice.dto.TableauBordDTO;
import com.anglikana.backoffice.dto.TotauxHierarchieDTO;
import com.anglikana.backoffice.entity.Ressource;
import com.anglikana.backoffice.mapper.ClergeMapper;
import com.anglikana.backoffice.mapper.EgliseMapper;
import com.anglikana.backoffice.mapper.RessourceMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository.LigneStatistique;
import com.anglikana.backoffice.repository.HierarchieStatistiqueRepository.Responsabilite;
import com.anglikana.backoffice.repository.RessourceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Tableaux de bord hiérarchiques : statistiques des églises, du clergé et des ressources,
 * calculées par PostgreSQL à partir des données réelles.
 *
 * <p>Règles de comptage :</p>
 * <ul>
 *     <li>une entité compte uniquement ses descendants (les niveaux égaux ou supérieurs valent 0) ;</li>
 *     <li>le clergé d'une entité est l'ensemble des membres DISTINCTS responsables de l'entité
 *     ou de l'un de ses descendants (diocèse → église) : un membre responsable de plusieurs
 *     entités n'est compté qu'une fois ;</li>
 *     <li>les ressources comptées sont celles des églises du périmètre.</li>
 * </ul>
 */
@Service
public class TableauBordService {

    /** Nombre maximal de niveaux inférieurs proposés pour la liste des enfants. */
    private static final int NOMBRE_VUES_ENFANTS = 2;

    private static final Collator ORDRE_FRANCAIS = Collator.getInstance(Locale.FRENCH);

    private final HierarchieStatistiqueRepository statistiqueRepository;
    private final ClergeRepository clergeRepository;
    private final EgliseRepository egliseRepository;
    private final RessourceRepository ressourceRepository;

    public TableauBordService(HierarchieStatistiqueRepository statistiqueRepository,
                              ClergeRepository clergeRepository,
                              EgliseRepository egliseRepository,
                              RessourceRepository ressourceRepository) {
        this.statistiqueRepository = statistiqueRepository;
        this.clergeRepository = clergeRepository;
        this.egliseRepository = egliseRepository;
        this.ressourceRepository = ressourceRepository;
    }

    /** Vue d'ensemble : totaux de la hiérarchie et statistiques de chaque diocèse. */
    @Transactional(readOnly = true)
    public ApercuHierarchieDTO getApercu() {
        LigneStatistique totaux = statistiqueRepository.totaux();
        List<LigneStatistique> lignesDioceses = statistiqueRepository.statistiquesParEntite(NiveauHierarchique.DIOCESE, null, null);
        Map<Integer, ClergeDTO> clerges = chargerClerges(responsablesDe(lignesDioceses));

        TotauxHierarchieDTO totauxDTO = new TotauxHierarchieDTO();
        totauxDTO.setDioceses(totaux.dioceses());
        totauxDTO.setRegions(totaux.regions());
        totauxDTO.setDistricts(totaux.districts());
        totauxDTO.setParoisses(totaux.paroisses());
        totauxDTO.setEglises(totaux.eglises());
        totauxDTO.setRessources(totaux.ressources());
        totauxDTO.setClerges(totaux.clerges());

        ApercuHierarchieDTO apercu = new ApercuHierarchieDTO();
        apercu.setTotaux(totauxDTO);
        apercu.setDioceses(versNoeuds(NiveauHierarchique.DIOCESE, lignesDioceses, clerges));
        return apercu;
    }

    /** Statistiques d'une entité ; vide si l'entité n'existe pas. */
    @Transactional(readOnly = true)
    public Optional<StatistiquesEntiteDTO> getStatistiques(NiveauHierarchique niveau, Integer id) {
        return statistiqueRepository.statistiquesParEntite(niveau, niveau, id).stream()
                .findFirst()
                .map(ligne -> versStatistiques(niveau, ligne));
    }

    /**
     * Tableau de bord d'une entité.
     *
     * @param niveauEnfantsDemande niveau des entités à lister ; s'il ne fait pas partie des vues
     *                             proposées (ou s'il est null), le niveau directement inférieur est utilisé
     * @return vide si l'entité n'existe pas
     */
    @Transactional(readOnly = true)
    public Optional<TableauBordDTO> getTableauBord(NiveauHierarchique niveau, Integer id,
                                                   NiveauHierarchique niveauEnfantsDemande) {
        List<ReferenceEntiteDTO> chaine = statistiqueRepository.chaine(niveau, id);
        if (chaine.isEmpty()) {
            return Optional.empty();
        }

        LigneStatistique ligneEntite = statistiqueRepository.statistiquesParEntite(niveau, niveau, id).get(0);

        List<NiveauHierarchique> inferieurs = niveau.inferieurs();
        List<NiveauHierarchique> vuesEnfants = List.copyOf(inferieurs.subList(0, Math.min(NOMBRE_VUES_ENFANTS, inferieurs.size())));
        NiveauHierarchique niveauEnfants = niveauEnfantsDemande != null && vuesEnfants.contains(niveauEnfantsDemande)
                ? niveauEnfantsDemande
                : vuesEnfants.stream().findFirst().orElse(null);

        List<LigneStatistique> lignesEnfants = niveauEnfants != null
                ? statistiqueRepository.statistiquesParEntite(niveauEnfants, niveau, id)
                : List.of();
        List<Responsabilite> responsabilites = statistiqueRepository.responsabilites(niveau, id);

        // Chargement groupé de tous les membres du clergé référencés (évite le N+1).
        Set<Integer> idsClerges = new HashSet<>(responsablesDe(lignesEnfants));
        if (ligneEntite.responsableId() != null) {
            idsClerges.add(ligneEntite.responsableId());
        }
        responsabilites.forEach(responsabilite -> idsClerges.add(responsabilite.clergeId()));
        Map<Integer, ClergeDTO> clerges = chargerClerges(idsClerges);

        TableauBordDTO tableauBord = new TableauBordDTO();
        tableauBord.setEntite(versNoeud(niveau, ligneEntite, clerges));
        tableauBord.setAncetres(List.copyOf(chaine.subList(0, chaine.size() - 1)));
        tableauBord.setNiveauEnfants(niveauEnfants);
        tableauBord.setVuesEnfants(vuesEnfants);
        tableauBord.setEnfants(niveauEnfants != null ? versNoeuds(niveauEnfants, lignesEnfants, clerges) : List.of());
        tableauBord.setClerges(versClergesAffectes(responsabilites, clerges));

        if (niveau == NiveauHierarchique.EGLISE) {
            tableauBord.setEglise(egliseRepository.findById(id).map(EgliseMapper::toDTO).orElse(null));
            tableauBord.setRessources(ressourceRepository.findDetailleesByEgliseId(id).stream()
                    .map(TableauBordService::versRessourceDetaillee)
                    .toList());
        } else {
            tableauBord.setEglise(null);
            tableauBord.setRessources(List.of());
        }

        return Optional.of(tableauBord);
    }

    /** Conserve uniquement les comptages des niveaux strictement inférieurs à celui de l'entité. */
    static StatistiquesEntiteDTO versStatistiques(NiveauHierarchique niveau, LigneStatistique ligne) {
        StatistiquesEntiteDTO statistiques = new StatistiquesEntiteDTO();
        statistiques.setRegions(niveau.estSuperieurA(NiveauHierarchique.REGION) ? ligne.regions() : 0);
        statistiques.setDistricts(niveau.estSuperieurA(NiveauHierarchique.DISTRICT) ? ligne.districts() : 0);
        statistiques.setParoisses(niveau.estSuperieurA(NiveauHierarchique.PAROISSE) ? ligne.paroisses() : 0);
        statistiques.setEglises(niveau.estSuperieurA(NiveauHierarchique.EGLISE) ? ligne.eglises() : 0);
        statistiques.setRessources(ligne.ressources());
        statistiques.setClerges(ligne.clerges());
        return statistiques;
    }

    private List<NoeudHierarchiqueDTO> versNoeuds(NiveauHierarchique niveau, List<LigneStatistique> lignes,
                                                  Map<Integer, ClergeDTO> clerges) {
        return lignes.stream()
                .map(ligne -> versNoeud(niveau, ligne, clerges))
                .sorted(Comparator.comparing(NoeudHierarchiqueDTO::getNom, Comparator.nullsLast(ORDRE_FRANCAIS)))
                .toList();
    }

    private NoeudHierarchiqueDTO versNoeud(NiveauHierarchique niveau, LigneStatistique ligne, Map<Integer, ClergeDTO> clerges) {
        NoeudHierarchiqueDTO noeud = new NoeudHierarchiqueDTO();
        noeud.setNiveau(niveau);
        noeud.setId(ligne.id());
        noeud.setNom(ligne.nom());
        noeud.setStatistiques(versStatistiques(niveau, ligne));
        noeud.setResponsable(ligne.responsableId() != null ? clerges.get(ligne.responsableId()) : null);
        return noeud;
    }

    /** Regroupe les responsabilités par membre du clergé, dans l'ordre hiérarchique. */
    private List<ClergeAffecteDTO> versClergesAffectes(List<Responsabilite> responsabilites, Map<Integer, ClergeDTO> clerges) {
        Map<Integer, List<String>> parClerge = new LinkedHashMap<>();
        for (Responsabilite responsabilite : responsabilites) {
            parClerge.computeIfAbsent(responsabilite.clergeId(), cle -> new ArrayList<>()).add(responsabilite.entiteNom());
        }

        List<ClergeAffecteDTO> affectes = new ArrayList<>();
        parClerge.forEach((clergeId, noms) -> {
            ClergeDTO clerge = clerges.get(clergeId);
            if (clerge != null) {
                affectes.add(new ClergeAffecteDTO(clerge, noms));
            }
        });
        return affectes;
    }

    private static RessourceDetailleeDTO versRessourceDetaillee(Ressource ressource) {
        RessourceDetailleeDTO dto = new RessourceDetailleeDTO();
        dto.setRessource(RessourceMapper.toDTO(ressource));
        dto.setTypeNom(ressource.getType() != null ? ressource.getType().getNom() : null);
        dto.setResponsable(ressource.getResponsable() != null ? ClergeMapper.toDTO(ressource.getResponsable()) : null);
        return dto;
    }

    private Map<Integer, ClergeDTO> chargerClerges(Collection<Integer> ids) {
        Map<Integer, ClergeDTO> clerges = new HashMap<>();
        if (!ids.isEmpty()) {
            clergeRepository.findAllById(ids).forEach(clerge -> clerges.put(clerge.getId(), ClergeMapper.toDTO(clerge)));
        }
        return clerges;
    }

    private static Set<Integer> responsablesDe(List<LigneStatistique> lignes) {
        Set<Integer> ids = new HashSet<>();
        lignes.stream().map(LigneStatistique::responsableId).filter(Objects::nonNull).forEach(ids::add);
        return ids;
    }
}
