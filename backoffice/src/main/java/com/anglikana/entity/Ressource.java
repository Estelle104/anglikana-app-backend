package com.anglikana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "ressource")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ressource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 255)
    private String adresse;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point localisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private TypeRessource type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eglise_id", nullable = false)
    private Eglise eglise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;
}