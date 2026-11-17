package com.techmentor.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "inscricoes_oportunidade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "oportunidade_id"}))
@Getter @Setter @NoArgsConstructor
public class InscricaoOportunidade {
    @Schema(description = "Situação da candidatura/inscrição: ENVIADA, EM_ANALISE, APROVADA ou RECUSADA")
    public enum Status { ENVIADA, EM_ANALISE, APROVADA, RECUSADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Oportunidade oportunidade;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.ENVIADA;
    private LocalDateTime criadaEm = LocalDateTime.now();

    // O que a candidata preencheu no formulário (vagas e bolsas). Nulos nos eventos e nas candidaturas antigas.
    @Column(length = 120) private String contatoNome;
    @Column(length = 160) private String contatoEmail;
    @Column(length = 20) private String telefone;
    @Column(length = 120) private String cidade;
    @Column(length = 255) private String linkProfissional;
    @Column(length = 500) private String mensagem;

    // Currículo em PDF (opcional): o nome do arquivo guardado no servidor e o nome original, para mostrar à equipe.
    // O arquivo nunca é público: só a administradora baixa, por uma rota própria.
    @Column(length = 100) private String curriculoArquivo;
    @Column(length = 120) private String curriculoNome;
}
