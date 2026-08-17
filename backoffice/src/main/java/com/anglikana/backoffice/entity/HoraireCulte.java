package com.anglikana.backoffice.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "horaire_culte")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HoraireCulte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 20)
    private String jour;

    @Column(length = 150)
    private String libelle;

    private LocalTime debut;

    private LocalTime fin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eglise_id", nullable = false)
    private Eglise eglise;
}