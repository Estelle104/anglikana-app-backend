package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "ressource")
// Suppression logique : les lignes à etat = -1 sont ignorées par toutes les requêtes JPA,
// et deleteById passe etat à -1 au lieu de supprimer la ligne.
@SQLRestriction("etat = 1")
@SQLDelete(sql = "UPDATE ressource SET etat = -1 WHERE id = ?")
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

    /** 1 = actif, -1 = supprimé (suppression logique). Non exposé dans les DTO. */
    @Builder.Default
    @ColumnDefault("1")
    @Column(nullable = false)
    private Short etat = 1;
}
