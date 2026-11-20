package com.anglikana.backoffice.service;

import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Role;
import com.anglikana.backoffice.entity.Utilisateur;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.UtilisateurRepository;
import com.anglikana.backoffice.security.UtilisateurConnecte;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final ClergeRepository clergeRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository,
                                    ClergeRepository clergeRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.clergeRepository = clergeRepository;
    }

    /**
     * Charge le compte et les droits de son clergé (niveau du rôle).
     * Le compte est désactivé si actif = false ou si son clergé est supprimé (etat = -1).
     */
    @Override
    @Transactional(readOnly = true)
    public UtilisateurConnecte loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Utilisateur utilisateur = utilisateurRepository.findByNomUtilisateur(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        // getId() sur le proxy LAZY ne charge pas le clergé ; findAvecRoleById ignore un clergé supprimé.
        Integer clergeId = utilisateur.getClerge() != null ? utilisateur.getClerge().getId() : null;
        Optional<Clerge> clerge = clergeId != null ? clergeRepository.findAvecRoleById(clergeId) : Optional.empty();

        boolean actif = Boolean.TRUE.equals(utilisateur.getActif()) && (clergeId == null || clerge.isPresent());
        Role role = clerge.map(Clerge::getRole).orElse(null);
        int niveau = role != null && role.getNiveau() != null ? role.getNiveau() : 0;

        return new UtilisateurConnecte(utilisateur.getNomUtilisateur(), utilisateur.getMotDePasse(), actif,
                clerge.map(Clerge::getId).orElse(null), niveau, role != null ? role.getNom() : null);
    }
}
