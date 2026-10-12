package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "bloqueios_usuario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bloqueador_id", "bloqueado_id"}))
@Getter @Setter @NoArgsConstructor
public class BloqueioUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario bloqueador;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario bloqueado;

    private LocalDateTime criadoEm = LocalDateTime.now();
}
