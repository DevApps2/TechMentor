package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

/** Uma linha por aula concluída. "8 de 18 aulas" = count(ProgressoAula) do curso / total de aulas. */
@Entity
@Table(name = "progresso_aulas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "aula_id"}))
@Getter @Setter @NoArgsConstructor
public class ProgressoAula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Aula aula;

    private LocalDateTime concluidaEm = LocalDateTime.now();
}
