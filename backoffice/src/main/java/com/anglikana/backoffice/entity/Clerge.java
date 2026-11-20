package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDate;

@Entity
@Table(name = "clerge")
// Suppression logique : les lignes à etat = -1 sont ignorées par toutes les requêtes JPA,
// et deleteById passe etat à -1 au lieu de supprimer la ligne.
@SQLRestriction("etat = 1")
@SQLDelete(sql = "UPDATE clerge SET etat = -1 WHERE id = ?")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Clerge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 100)
    private String hierarchie;

    @Column(length = 30)
    private String telephone;

    @Column(length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    /** 1 = actif, -1 = supprimé (suppression logique). Non exposé dans les DTO. */
    @Builder.Default
    @ColumnDefault("1")
    @Column(nullable = false)
    private Short etat = 1;
}
