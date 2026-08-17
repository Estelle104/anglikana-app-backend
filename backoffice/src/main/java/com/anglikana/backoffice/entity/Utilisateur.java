package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utilisateur")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom_utilisateur", nullable = false, unique = true, length = 100)
    private String nomUtilisateur;

    @Column(name = "mot_de_passe", nullable = false, length = 255)
    private String motDePasse;

    @Builder.Default
    private Boolean actif = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clerge_id", unique = true)
    private Clerge clerge;
}