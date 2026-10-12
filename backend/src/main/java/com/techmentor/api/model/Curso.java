package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "cursos")
@Getter @Setter @NoArgsConstructor
public class Curso {
    public enum Categoria { PROGRAMACAO, DADOS, DESIGN, CARREIRA, OUTROS }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String titulo;
    @Column(length = 1000) private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Categoria categoria;
    private String iconeUrl;
    private boolean ativo = true;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Modulo> modulos = new ArrayList<>();
}
