package com.anglikana.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contribution")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_entite", nullable = false, length = 50)
    private String typeEntite; // "diocese", "region", "eglise", etc.

    @Column(name = "entite_id", nullable = false)
    private Integer entiteId;

    @Column(length = 50)
    private String action;

    @Column(name = "date_contribution")
    @Builder.Default
    private LocalDateTime dateContribution = LocalDateTime.now();

    @Column(length = 50)
    private String statut;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private Utilisateur validateur;
}