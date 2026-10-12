package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

/** Orientações de segurança e canais de ajuda exibidos na tela de Segurança. */
@Entity @Table(name = "conteudos_seguranca")
@Getter @Setter @NoArgsConstructor
public class ConteudoSeguranca {
    public enum Tipo { ORIENTACAO, CANAL_AJUDA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Tipo tipo;
    @Column(nullable = false) private String titulo;
    @Column(length = 1000) private String descricao;
    private String contato; // telefone, e-mail ou link (só para canais de ajuda)
}
