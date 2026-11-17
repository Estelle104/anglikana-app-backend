package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "region")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    /** Limite de la zone : polygone importé depuis QGIS (SRID 4326). */
    @Column(name = "carte_qgis", columnDefinition = "geometry(Polygon,4326)")
    private Polygon carteQgis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diocese_id", nullable = false)
    private Diocese diocese;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;
}