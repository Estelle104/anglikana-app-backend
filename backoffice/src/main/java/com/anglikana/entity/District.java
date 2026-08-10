package com.anglikana.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "district")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(name = "carte_qgis", length = 255)
    private String carteQgis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;
}