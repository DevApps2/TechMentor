package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Uma linha do histórico: quem fez o quê, em qual curso (ou parceira) e quando.
 * Só se acrescenta; nunca se edita nem se apaga. Nome e papel de quem fez ficam copiados aqui,
 * para o histórico continuar legível mesmo que a conta mude depois.
 */
@Entity @Table(name = "auditoria")
@Getter @Setter @NoArgsConstructor
public class RegistroAuditoria {

    public static final String CURSO = "CURSO";
    public static final String PARCEIRA = "PARCEIRA";
    public static final String PEDIDO = "PEDIDO";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false) private LocalDateTime quando = LocalDateTime.now();

    // Nulos quando foi o próprio sistema
    private Long autorId;
    @Column(length = 120) private String autorNome;
    @Column(length = 20) private String autorPapel;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AcaoAuditada acao;

    @Column(nullable = false, length = 20) private String entidade;
    @Column(nullable = false) private Long entidadeId;

    @Column(length = 500) private String detalhe;
}
