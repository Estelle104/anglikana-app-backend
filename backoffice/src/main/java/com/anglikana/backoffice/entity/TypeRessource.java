package com.anglikana.backoffice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_ressource")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TypeRessource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;
}