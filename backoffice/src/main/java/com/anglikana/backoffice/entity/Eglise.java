package com.anglikana.backoffice.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "eglise")
// Suppression logique : les lignes à etat = -1 sont ignorées par toutes les requêtes JPA,
// et deleteById passe etat à -1 au lieu de supprimer la ligne.
@SQLRestriction("etat = 1")
@SQLDelete(sql = "UPDATE eglise SET etat = -1 WHERE id = ?")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Eglise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 255)
    private String adresse;

    // Nécessite hibernate-spatial + org.locationtech.jts
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point localisation;

    @Column(columnDefinition = "TEXT")
    private String historique;

    @Column(name = "lien_facebook", length = 255)
    private String lienFacebook;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Clerge responsable;

    /** 1 = actif, -1 = supprimé (suppression logique). Non exposé dans les DTO. */
    @Builder.Default
    @ColumnDefault("1")
    @Column(nullable = false)
    private Short etat = 1;
}
