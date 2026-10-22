package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonPropertyOrder({"id", "conquista", "desbloqueadaEm"})
public class UsuarioConquista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Conquista conquista;

    private LocalDateTime desbloqueadaEm = LocalDateTime.now();
}