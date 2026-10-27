package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Transient;


import java.util.ArrayList;
import java.util.List;


@Entity @Table(name = "cursos")
@Getter @Setter @NoArgsConstructor

public class Curso {

    @JsonPropertyOrder({"id", "titulo", "descricao", "categoria", "iconeUrl", "ativo", "modulos"})
    @JsonInclude(JsonInclude.Include.NON_NULL)

    @Transient
    private Long totalAulas;
    @Schema(description = "Categoria do curso: PROGRAMACAO, DADOS, DESIGN, CARREIRA ou OUTROS")
    public enum Categoria { PROGRAMACAO, DADOS, DESIGN, CARREIRA, OUTROS }


    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Modulo> modulos = new ArrayList<>();


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String titulo;
    @Column(length = 1000) private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Categoria categoria;
    @Column(length = 255) private String iconeUrl;
    @Column(nullable = false) private boolean ativo = true;
}