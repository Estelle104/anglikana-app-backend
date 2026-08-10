package com.anglikana.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "region")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diocese_id", nullable = false)
    private Diocese diocese;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;
}