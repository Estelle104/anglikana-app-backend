package com.anglikana.backoffice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Utilisateur connecté, rechargé depuis la base à chaque requête par le filtre JWT.
 *
 * <p>Ses droits viennent de son clergé : le {@link #getNiveau() niveau} de son rôle
 * ({@link LevelRequired}) et les entités dont il est responsable ({@link NeedAuthorisation}).
 * Autorité unique : {@code NIVEAU_<n>} (0 sans clergé ou sans rôle).</p>
 */
public class UtilisateurConnecte implements UserDetails {

    private final String nomUtilisateur;
    private final String motDePasse;
    private final boolean actif;
    private final Integer clergeId;
    private final int niveau;
    private final String roleNom;

    public UtilisateurConnecte(String nomUtilisateur, String motDePasse, boolean actif,
                               Integer clergeId, int niveau, String roleNom) {
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse = motDePasse;
        this.actif = actif;
        this.clergeId = clergeId;
        this.niveau = niveau;
        this.roleNom = roleNom;
    }

    /** Clergé lié au compte ; null si le compte n'en a pas. */
    public Integer getClergeId() { return clergeId; }

    /** Niveau du rôle du clergé : 5 = Évêque ... 1 = Katekista, 0 sans rôle. */
    public int getNiveau() { return niveau; }

    /** Nom du rôle (« Évêque »...) ; null sans rôle. */
    public String getRoleNom() { return roleNom; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("NIVEAU_" + niveau));
    }

    @Override
    public String getPassword() { return motDePasse; }

    @Override
    public String getUsername() { return nomUtilisateur; }

    @Override
    public boolean isEnabled() { return actif; }
}
