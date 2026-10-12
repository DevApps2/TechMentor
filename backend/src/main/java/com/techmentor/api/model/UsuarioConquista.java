package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "usuario_conquistas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "conquista_id"}))
@Getter @Setter @NoArgsConstructor
public class UsuarioConquista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Conquista conquista;

    private LocalDateTime desbloqueadaEm = LocalDateTime.now();
}
