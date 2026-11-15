package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** O código que a administradora manda por e-mail para a parceira criar a senha. Só o hash fica guardado. */
@Entity @Table(name = "convites_parceira")
@Getter @Setter @NoArgsConstructor
public class ConviteParceira {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parceira_id")
    private Parceira parceira;

    @Column(nullable = false) private String codigoHash;

    @Column(nullable = false) private LocalDateTime expiraEm;

    @Column(nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false) private int tentativas = 0;

    @Column(nullable = false) private boolean usado = false;
}
