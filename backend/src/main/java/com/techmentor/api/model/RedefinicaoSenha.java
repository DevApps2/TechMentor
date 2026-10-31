package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "redefinicoes_senha")
@Getter @Setter @NoArgsConstructor
public class RedefinicaoSenha {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Hash BCrypt do código de 6 dígitos: o código puro nunca é guardado
    @Column(nullable = false) private String codigoHash;

    @Column(nullable = false) private LocalDateTime expiraEm;

    @Column(nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false) private int tentativas = 0;

    @Column(nullable = false) private boolean usado = false;
}