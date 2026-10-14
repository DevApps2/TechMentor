package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "cursos")
@Getter @Setter @NoArgsConstructor
public class Curso {
    public enum Categoria { PROGRAMACAO, DADOS, DESIGN, CARREIRA, OUTROS }
    // Corrigi aqui, tirei o mudolo e arrumei um loop do JSON
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String titulo;
    @Column(length = 1000) private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Categoria categoria;
    @Column(length = 255) private String iconeUrl;
    @Column(nullable = false) private boolean ativo = true;
}