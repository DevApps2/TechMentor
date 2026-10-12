package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "avaliacoes_mentoria")
@Getter @Setter @NoArgsConstructor
public class AvaliacaoMentoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentoria_id", unique = true)
    private Mentoria mentoria;

    @Column(nullable = false) private int nota; // 1 a 5
    @Column(length = 1000) private String comentario;
    @Column(updatable = false) private LocalDateTime criadaEm = LocalDateTime.now();
}
