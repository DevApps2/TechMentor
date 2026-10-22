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
@JsonPropertyOrder({"id", "titulo", "mensagem", "lida", "criadaEm"})
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;

    @Column(nullable = false) private String titulo;
    @Column(length = 500) private String mensagem;
    private boolean lida = false;
    private LocalDateTime criadaEm = LocalDateTime.now();
}