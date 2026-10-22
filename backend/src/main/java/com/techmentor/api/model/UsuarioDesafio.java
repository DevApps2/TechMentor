package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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

    @JsonPropertyOrder({"id", "desafio", "status", "concluidoEm"})
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long aulasConcluidas;
    public enum Status { EM_ANDAMENTO, CONCLUIDO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Desafio desafio;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.EM_ANDAMENTO;
    private LocalDateTime concluidoEm;
}
