package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "aulas")
@Getter @Setter @NoArgsConstructor
public class Aula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    @Column(nullable = false) private String titulo;
    @Column(nullable = false) private int ordem;
    private int duracaoMinutos;
    private int xpRecompensa = 20;
    private String conteudoUrl;
}
