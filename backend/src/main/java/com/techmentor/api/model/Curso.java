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
    @Schema(description = "Categoria do curso: PROGRAMACAO, DADOS, DESIGN, CARREIRA, SEGURANCA, IA, REDES ou OUTROS")
    public enum Categoria { PROGRAMACAO, DADOS, DESIGN, CARREIRA, OUTROS, SEGURANCA, IA, REDES }

    @Schema(description = "Nível da trilha: INICIANTE, INTERMEDIARIO ou AVANCADO")
    public enum Nivel { INICIANTE, INTERMEDIARIO, AVANCADO }


    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Modulo> modulos = new ArrayList<>();


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String titulo;
    @Column(length = 1000) private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Categoria categoria;
    @Enumerated(EnumType.STRING) @Column(length = 20) private Nivel nivel;
    @Column(length = 255) private String iconeUrl;
    // Quem produziu o conteúdo (ex.: "Curso em Vídeo"). Aparece como crédito nas telas da aluna.
    @Column(length = 120) private String instituicao;

    // Nulo nos cursos antigos: nesse caso a situação sai do "ativo" (veja getSituacao)
    @Enumerated(EnumType.STRING) @Column(length = 20) private SituacaoCurso situacao;

    // Espelho da situação (ativo = PUBLICADO). Fica na tabela por compatibilidade e é o que as consultas das alunas usam.
    @Column(nullable = false) private boolean ativo = true;

    /** A situação do curso; nos cursos antigos (sem o campo preenchido), ativo = PUBLICADO e inativo = ARQUIVADO. */
    public SituacaoCurso getSituacao() {
        if (situacao != null) return situacao;
        return ativo ? SituacaoCurso.PUBLICADO : SituacaoCurso.ARQUIVADO;
    }

    public void setSituacao(SituacaoCurso nova) {
        this.situacao = nova;
        this.ativo = nova == SituacaoCurso.PUBLICADO;
    }

    /** Compatibilidade com o que já existia: ativo = publicado, inativo = arquivado. */
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
        this.situacao = ativo ? SituacaoCurso.PUBLICADO : SituacaoCurso.ARQUIVADO;
    }

    @PrePersist
    @PreUpdate
    void sincronizarSituacao() {
        this.situacao = getSituacao();
        this.ativo = this.situacao == SituacaoCurso.PUBLICADO;
    }
}