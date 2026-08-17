package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diocese")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Diocese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(name = "carte_qgis", length = 255)
    private String carteQgis;

    @Column(columnDefinition = "TEXT")
    private String presentation;

    @Column(length = 100)
    private String coordonnees;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;
}