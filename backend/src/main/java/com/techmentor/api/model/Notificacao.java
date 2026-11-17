package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "notificacoes")
@Getter @Setter @NoArgsConstructor
@JsonPropertyOrder({"id", "titulo", "mensagem", "tipo", "destino", "referencia", "lida", "criadaEm"})
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;

    @Column(nullable = false) private String titulo;
    @Column(length = 500) private String mensagem;

    // Nulos nas notificações antigas: os getters abaixo devolvem GERAL / NENHUM
    @Enumerated(EnumType.STRING) @Column(length = 30) private TipoNotificacao tipo;
    @Enumerated(EnumType.STRING) @Column(length = 30) private DestinoNotificacao destino;
    // Completa o destino (título do curso, id do curso...); nulo quando o destino não precisa
    @Column(length = 150) private String referencia;

    private boolean lida = false;
    private LocalDateTime criadaEm = LocalDateTime.now();

    public TipoNotificacao getTipo() {
        return tipo == null ? TipoNotificacao.GERAL : tipo;
    }

    public DestinoNotificacao getDestino() {
        return destino == null ? DestinoNotificacao.NENHUM : destino;
    }
}
