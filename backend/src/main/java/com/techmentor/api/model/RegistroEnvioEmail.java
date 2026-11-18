package com.techmentor.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tentativas de e-mail dos eventos de parceria. Mensagens com códigos não guardam o conteúdo para nova tentativa. */
@Entity
@Table(name = "registros_envio_email",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_evento", columnNames = "chave_evento"),
        indexes = @Index(name = "ix_email_retentativa", columnList = "situacao, proxima_tentativa_em"))
@Getter
@Setter
@NoArgsConstructor
public class RegistroEnvioEmail {

    public enum Situacao { PENDENTE, ENVIANDO, ENVIADO, FALHOU }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_evento", nullable = false, length = 120)
    private String chaveEvento;

    @Column(nullable = false, length = 254)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Situacao situacao = Situacao.PENDENTE;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    @Column(name = "proxima_tentativa_em")
    private LocalDateTime proximaTentativaEm;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;

    // O HTML/texto de códigos de convite não é persistido: esses convites são renovados pelo endpoint rate-limited.
    @Lob
    @Column(name = "texto_retentativa")
    private String textoRetentativa;

    @Lob
    @Column(name = "html_retentativa")
    private String htmlRetentativa;

    @Column(name = "com_logo", nullable = false)
    private boolean comLogo;

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadaEm == null) criadaEm = agora;
        atualizadaEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadaEm = LocalDateTime.now();
    }
}
