package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "notificacoes")
@Getter @Setter @NoArgsConstructor
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;

    @Column(nullable = false) private String titulo;
    @Column(length = 500) private String mensagem;
    private boolean lida = false;
    private LocalDateTime criadaEm = LocalDateTime.now();
}
