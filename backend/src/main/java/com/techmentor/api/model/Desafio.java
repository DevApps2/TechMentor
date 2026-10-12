package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "desafios")
@Getter @Setter @NoArgsConstructor
public class Desafio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String titulo;
    @Column(length = 1000) private String descricao;
    private int xpRecompensa;
    private LocalDate inicio;
    private LocalDate fim;
}
