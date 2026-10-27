package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@JsonPropertyOrder({"id", "nome", "descricao", "iconeUrl", "xpBonus"})

/** Medalha que pode ser desbloqueada. */
@Entity @Table(name = "conquistas")
@Getter @Setter @NoArgsConstructor
public class Conquista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String nome;
    @Column(length = 500) private String descricao;
    private String iconeUrl;
    private int xpBonus = 0;
}
