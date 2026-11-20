package com.anglikana.backoffice.security;

import com.anglikana.backoffice.dto.NiveauHierarchique;
import com.anglikana.backoffice.dto.ReferenceEntiteDTO;
import com.anglikana.backoffice.repository.PerimetreRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Règles d'accès par niveau et par périmètre, partagées par les annotations
 * ({@link PerimetreAuthorizationManager}) et par les contrôles faits dans le code (rattachement
 * d'une entité à un nouveau parent).
 */
@Service
@Transactional(readOnly = true)
public class AutorisationService {

    private final PerimetreRepository perimetreRepository;

    public AutorisationService(PerimetreRepository perimetreRepository) {
        this.perimetreRepository = perimetreRepository;
    }

    /** Utilisateur authentifié et actif ; vide pour un appel anonyme. */
    public static Optional<UtilisateurConnecte> utilisateur(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UtilisateurConnecte utilisateur
                && utilisateur.isEnabled()) {
            return Optional.of(utilisateur);
        }
        return Optional.empty();
    }

    public static Optional<UtilisateurConnecte> utilisateurCourant() {
        return utilisateur(SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean aLeNiveau(UtilisateurConnecte utilisateur, int niveauRequis) {
        return utilisateur.getNiveau() >= niveauRequis;
    }

    /**
     * Vrai si le clergé de l'utilisateur est responsable de l'entité (ou, avec {@code cascade},
     * de l'un de ses ancêtres).
     *
     * @throws EntiteIntrouvableException si l'entité n'existe pas ou est supprimée
     */
    public boolean estResponsable(UtilisateurConnecte utilisateur, EntiteProtegee entite, Integer id, boolean cascade) {
        if (id == null) {
            return false;
        }
        List<Integer> responsables = perimetreRepository.chaineResponsables(entite, id)
                .orElseThrow(() -> new EntiteIntrouvableException(entite, id));

        Integer clergeId = utilisateur.getClergeId();
        if (clergeId == null) {
            return false;
        }
        return cascade ? responsables.contains(clergeId) : Objects.equals(responsables.get(0), clergeId);
    }

    /**
     * Entités racines du périmètre de l'utilisateur : celles dont son clergé est responsable, sans
     * celles déjà comprises dans une autre racine. Vide sans clergé.
     */
    public List<ReferenceEntiteDTO> racines(UtilisateurConnecte utilisateur) {
        return utilisateur.getClergeId() != null ? perimetreRepository.racines(utilisateur.getClergeId()) : List.of();
    }

    /** Diocèses dont le clergé de l'utilisateur est responsable. */
    public List<Integer> dioceses(UtilisateurConnecte utilisateur) {
        return racines(utilisateur).stream()
                .filter(racine -> racine.getNiveau() == NiveauHierarchique.DIOCESE)
                .map(ReferenceEntiteDTO::getId)
                .toList();
    }

    /**
     * Contrôle fait dans le code pour l'utilisateur courant : refus (403) s'il n'est pas
     * responsable de l'entité ou d'un de ses ancêtres, 404 si l'entité n'existe pas.
     */
    public void verifier(EntiteProtegee entite, Integer id, boolean cascade) {
        UtilisateurConnecte utilisateur = utilisateurCourant()
                .orElseThrow(() -> new AccessDeniedException("Authentification requise"));
        if (!estResponsable(utilisateur, entite, id, cascade)) {
            throw new AccessDeniedException("Hors du périmètre : " + entite.getCode() + " " + id);
        }
    }
}
