package com.anglikana.backoffice.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "paroisse")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Paroisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(name = "carte_qgis", length = 255)
    private String carteQgis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;
}