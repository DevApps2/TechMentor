package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity @Table(name = "modulos")
@Getter @Setter @NoArgsConstructor
@JsonPropertyOrder({"id", "titulo", "ordem", "aulas"})

public class Modulo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    @JsonIgnore
    private Curso curso;

    @Column(nullable = false) private String titulo;

    @Column(nullable = false) private int ordem;

    @OneToMany(mappedBy = "modulo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Aula> aulas = new ArrayList<>();
}
