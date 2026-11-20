package com.anglikana.backoffice.service;

import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.dto.EgliseDTO;
import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ParoisseDTO;
import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.dto.RessourceDTO;
import com.anglikana.backoffice.dto.TypeRessourceDTO;
import com.anglikana.backoffice.mapper.DistrictMapper;
import com.anglikana.backoffice.mapper.EgliseMapper;
import com.anglikana.backoffice.mapper.ParoisseMapper;
import com.anglikana.backoffice.mapper.RegionMapper;
import com.anglikana.backoffice.mapper.RessourceMapper;
import com.anglikana.backoffice.mapper.TypeRessourceMapper;
import com.anglikana.backoffice.repository.DioceseRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.ParoisseRepository;
import com.anglikana.backoffice.repository.RegionRepository;
import com.anglikana.backoffice.repository.RessourceRepository;
import com.anglikana.backoffice.repository.TypeRessourceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Consultation hiérarchique : entités enfants d'une entité parente
 * (Diocèse → Région → District → Paroisse → Église → Ressources).
 * Chaque méthode renvoie un Optional vide lorsque l'entité parente n'existe pas.
 */
@Service
@Transactional(readOnly = true)
public class HierarchieService {

    private final DioceseRepository dioceseRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final ParoisseRepository paroisseRepository;
    private final EgliseRepository egliseRepository;
    private final RessourceRepository ressourceRepository;
    private final TypeRessourceRepository typeRessourceRepository;

    public HierarchieService(DioceseRepository dioceseRepository,
                             RegionRepository regionRepository,
                             DistrictRepository districtRepository,
                             ParoisseRepository paroisseRepository,
                             EgliseRepository egliseRepository,
                             RessourceRepository ressourceRepository,
                             TypeRessourceRepository typeRessourceRepository) {
        this.dioceseRepository = dioceseRepository;
        this.regionRepository = regionRepository;
        this.districtRepository = districtRepository;
        this.paroisseRepository = paroisseRepository;
        this.egliseRepository = egliseRepository;
        this.ressourceRepository = ressourceRepository;
        this.typeRessourceRepository = typeRessourceRepository;
    }

    public Optional<List<RegionDTO>> getRegionsDuDiocese(Integer dioceseId) {
        if (!dioceseRepository.existsById(dioceseId)) {
            return Optional.empty();
        }
        return Optional.of(regionRepository.findByDioceseIdOrderByNomAsc(dioceseId)
                .stream().map(RegionMapper::toDTO).toList());
    }

    /** Districts d'un diocèse, via ses régions (un district appartient à une région, elle-même rattachée au diocèse). */
    public Optional<List<DistrictDTO>> getDistrictsDuDiocese(Integer dioceseId) {
        if (!dioceseRepository.existsById(dioceseId)) {
            return Optional.empty();
        }
        return Optional.of(districtRepository.findByRegionDioceseIdOrderByNomAsc(dioceseId)
                .stream().map(DistrictMapper::toDTO).toList());
    }

    public Optional<List<DistrictDTO>> getDistrictsDeLaRegion(Integer regionId) {
        if (!regionRepository.existsById(regionId)) {
            return Optional.empty();
        }
        return Optional.of(districtRepository.findByRegionIdOrderByNomAsc(regionId)
                .stream().map(DistrictMapper::toDTO).toList());
    }

    public Optional<List<ParoisseDTO>> getParoissesDuDistrict(Integer districtId) {
        if (!districtRepository.existsById(districtId)) {
            return Optional.empty();
        }
        return Optional.of(paroisseRepository.findByDistrictIdOrderByNomAsc(districtId)
                .stream().map(ParoisseMapper::toDTO).toList());
    }

    public Optional<List<EgliseDTO>> getEglisesDeLaParoisse(Integer paroisseId) {
        if (!paroisseRepository.existsById(paroisseId)) {
            return Optional.empty();
        }
        return Optional.of(egliseRepository.findByParoisseIdOrderByNomAsc(paroisseId)
                .stream().map(EgliseMapper::toDTO).toList());
    }

    public Optional<List<RessourceDTO>> getRessourcesDeLEglise(Integer egliseId) {
        if (!egliseRepository.existsById(egliseId)) {
            return Optional.empty();
        }
        return Optional.of(ressourceRepository.findByEgliseIdOrderByNomAsc(egliseId)
                .stream().map(RessourceMapper::toDTO).toList());
    }

    /**
     * Motif de refus de la suppression logique d'une entité : elle a encore au moins une fille
     * directe active (les petites-filles ne sont pas regardées). Les filles d'une église sont ses
     * ressources. Vide si la suppression est possible.
     */
    public Optional<String> refusSuppression(NiveauHierarchique niveau, Integer id) {
        String motif = switch (niveau) {
            case DIOCESE -> regionRepository.existsByDioceseId(id)
                    ? "le diocèse contient encore au moins une région active" : null;
            case REGION -> districtRepository.existsByRegionId(id)
                    ? "la région contient encore au moins un district actif" : null;
            case DISTRICT -> paroisseRepository.existsByDistrictId(id)
                    ? "le district contient encore au moins une paroisse active" : null;
            case PAROISSE -> egliseRepository.existsByParoisseId(id)
                    ? "la paroisse contient encore au moins une église active" : null;
            case EGLISE -> ressourceRepository.existsByEgliseId(id)
                    ? "l'église contient encore au moins une ressource active" : null;
        };
        return Optional.ofNullable(motif).map(texte -> "Suppression impossible : " + texte + ".");
    }

    /** Vrai si le membre du clergé est responsable d'au moins une entité ou ressource active. */
    public boolean estResponsableActif(Integer clergeId) {
        return dioceseRepository.existsByResponsableId(clergeId)
                || regionRepository.existsByResponsableId(clergeId)
                || districtRepository.existsByResponsableId(clergeId)
                || paroisseRepository.existsByResponsableId(clergeId)
                || egliseRepository.existsByResponsableId(clergeId)
                || ressourceRepository.existsByResponsableId(clergeId);
    }

    public List<TypeRessourceDTO> getTypesRessource() {
        return typeRessourceRepository.findAllByOrderByNomAsc()
                .stream().map(TypeRessourceMapper::toDTO).toList();
    }

    public Optional<TypeRessourceDTO> getTypeRessource(Integer id) {
        return typeRessourceRepository.findById(id).map(TypeRessourceMapper::toDTO);
    }
}
