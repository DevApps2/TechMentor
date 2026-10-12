package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "usuario_desafios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "desafio_id"}))
@Getter @Setter @NoArgsConstructor
public class UsuarioDesafio {
    public enum Status { EM_ANDAMENTO, CONCLUIDO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Desafio desafio;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.EM_ANDAMENTO;
    private LocalDateTime concluidoEm;
}
