package com.techmentor.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "oportunidades")
@Getter @Setter @NoArgsConstructor
public class Oportunidade {
    @Schema(description = "Tipo da oportunidade: ESTAGIO, EMPREGO, BOLSA ou EVENTO")
    public enum Tipo { ESTAGIO, EMPREGO, BOLSA, EVENTO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Tipo tipo;
    @Column(nullable = false) private String titulo;
    private String organizacao;
    @Column(length = 2000) private String descricao;
    @Column(length = 2000) private String requisitos;
    @Column(length = 2000) private String beneficios;
    @Column(length = 80) private String area;
    @Column(length = 50) private String nivel;
    private String localidade;
    private String linkExterno;
    private LocalDate prazoInscricao;
    private boolean ativa = true;
}
