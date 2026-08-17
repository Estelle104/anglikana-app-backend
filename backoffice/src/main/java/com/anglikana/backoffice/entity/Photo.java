package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "photo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(length = 255)
    private String description;

    @Column(name = "date_ajout")
    @Builder.Default
    private LocalDate dateAjout = LocalDate.now();

    @Column(name = "type_entite", nullable = false, length = 50)
    private String typeEntite; // "eglise", "ressource", "clerge"

    @Column(name = "entite_id", nullable = false)
    private Integer entiteId;
}