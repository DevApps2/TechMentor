package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * O pedido que uma instituição faz pelo site ("Seja nosso parceiro"). Ainda não é uma conta: só vira Parceira
 * (com convite por e-mail) quando a administradora aprova.
 */
@Entity @Table(name = "pedidos_parceria")
@Getter @Setter @NoArgsConstructor
public class PedidoParceria {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false, length = 120) private String nome;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TipoParceira tipo;

    // Só os 14 dígitos; nulo para quem não tem CNPJ
    @Column(length = 14) private String documento;

    @Column(length = 200) private String site;

    @Column(nullable = false, length = 120) private String responsavel;

    @Column(nullable = false, length = 160) private String email;

    // O que a instituição pretende oferecer
    @Column(length = 1000) private String mensagem;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SituacaoPedido situacao = SituacaoPedido.PENDENTE;

    @Column(nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    // Prova do consentimento (LGPD): quando aceitou a política de privacidade e qual versão do texto estava no site
    @Column(nullable = false) private LocalDateTime consentimentoEm = LocalDateTime.now();
    @Column(nullable = false, length = 20) private String politicaVersao;

    private LocalDateTime decididoEm;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decidido_por_id") private Usuario decididoPor;
    @Column(length = 500) private String motivoDaRecusa;

    // A parceira criada quando o pedido é aprovado
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parceira_id") private Parceira parceira;
}
