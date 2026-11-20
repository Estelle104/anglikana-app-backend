package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "diocese")
// Suppression logique : les lignes à etat = -1 sont ignorées par toutes les requêtes JPA,
// et deleteById passe etat à -1 au lieu de supprimer la ligne.
@SQLRestriction("etat = 1")
@SQLDelete(sql = "UPDATE diocese SET etat = -1 WHERE id = ?")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Diocese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    /** Limite de la zone : polygone importé depuis QGIS (SRID 4326). */
    @Column(name = "carte_qgis", columnDefinition = "geometry(Polygon,4326)")
    private Polygon carteQgis;

    @Column(columnDefinition = "TEXT")
    private String presentation;

    @Column(length = 100)
    private String coordonnees;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;

    /** 1 = actif, -1 = supprimé (suppression logique). Non exposé dans les DTO. */
    @Builder.Default
    @ColumnDefault("1")
    @Column(nullable = false)
    private Short etat = 1;
}
