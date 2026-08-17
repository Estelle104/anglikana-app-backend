package com.anglikana.backoffice.service;

import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Role;
import com.anglikana.backoffice.entity.Utilisateur;
import com.anglikana.backoffice.repository.UtilisateurRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Utilisateur utilisateur = utilisateurRepository.findByNomUtilisateur(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        String roleNom = extraireRole(utilisateur);

        return User.builder().username(utilisateur.getNomUtilisateur())
                .password(utilisateur.getMotDePasse()).disabled(!Boolean.TRUE.equals(utilisateur.getActif()))
                .roles(roleNom).build();
    }

    private String extraireRole(Utilisateur utilisateur) {
        Clerge clerge = utilisateur.getClerge();
        if (clerge == null) {
            return "USER";
        }

        Role role = clerge.getRole();
        if (role == null || role.getNom() == null) {
            return "USER"; 
        }

        return role.getNom().toUpperCase();
    }
}