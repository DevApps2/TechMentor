package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Avaliação de uma mentoria concluída, de 1 a 5 estrelas. Cada pessoa avalia a outra, uma vez só:
 * a aluna avalia a mentora (pública, entra na média dela) e a mentora avalia a aluna (privada).
 * A restrição única garante "uma avaliação por pessoa por mentoria" mesmo se duas chamadas chegarem juntas.
 */
@Entity
@Table(name = "avaliacoes",
        uniqueConstraints = @UniqueConstraint(name = "uk_avaliacao_mentoria_avaliador",
                columnNames = {"mentoria_id", "avaliador_id"}))
@Getter @Setter @NoArgsConstructor
public class Avaliacao {

    /** Quem escreveu a avaliação. */
    public enum Autor { ALUNA, MENTORA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentoria_id")
    private Mentoria mentoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliador_id")
    private Usuario avaliador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliado_id")
    private Usuario avaliado;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Autor autor;

    @Column(nullable = false) private int nota; // 1 a 5

    @Column(length = 1000) private String comentario;

    // Comentário escondido pela moderação (a nota continua valendo na média)
    @Column(nullable = false) private boolean oculta = false;

    @Column(updatable = false) private LocalDateTime criadaEm = LocalDateTime.now();
}