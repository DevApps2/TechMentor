package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "desafios")
@Getter @Setter @NoArgsConstructor
@JsonPropertyOrder({"id", "titulo", "descricao", "xpRecompensa", "inicio", "fim"})
public class Desafio {
    /** O que o desafio conta: AULAS concluídas ou MENTORIAS agendadas dentro do período. */
    public enum Tipo { AULAS, MENTORIA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String titulo;
    @Column(length = 1000) private String descricao;
    private int xpRecompensa;
    private LocalDate inicio;
    private LocalDate fim;
    private int metaAulas = 1; // meta de aulas (tipo AULAS) ou de mentorias (tipo MENTORIA)
    @Enumerated(EnumType.STRING) @Column(length = 20) private Tipo tipo = Tipo.AULAS;
}
