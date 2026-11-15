package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** O perfil da empresa (ou ONG, ou pessoa) que cadastra cursos. A conta de acesso é um Usuario do tipo PARCEIRA. */
@Entity @Table(name = "parceiras")
@Getter @Setter @NoArgsConstructor
public class Parceira {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    // O nome que as alunas veem no crédito do curso
    @Column(nullable = false, unique = true, length = 120) private String nome;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TipoParceira tipo;

    // Só os 14 dígitos do CNPJ; nulo para quem não tem
    @Column(unique = true, length = 14) private String documento;

    @Column(length = 200) private String site;

    @Column(nullable = false, length = 120) private String responsavel;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SituacaoParceira situacao = SituacaoParceira.CONVIDADA;

    @Column(nullable = false) private LocalDateTime criadaEm = LocalDateTime.now();

    // Termos de Parceria: a versão que ela aceitou e quando
    @Column(length = 20) private String termosVersao;
    private LocalDateTime termosAceitosEm;

    // Checklist da verificação feita pela administradora
    private boolean siteConferido;
    private boolean dominioDoEmailConferido;
    private boolean identificacaoConferida;
    private LocalDateTime verificadaEm;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "verificada_por_id") private Usuario verificadaPor;
    @Column(length = 500) private String observacaoDaVerificacao;

    @Column(length = 500) private String motivoDaSuspensao;

    public boolean aceitouOsTermos() {
        return termosAceitosEm != null;
    }
}
