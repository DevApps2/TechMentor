package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity
@Table(name = "matriculas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "curso_id"}))
@Getter @Setter @NoArgsConstructor
@JsonPropertyOrder({"id", "usuario", "curso", "matriculadaEm", "concluidoEm"})
public class Matricula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Curso curso;

    private LocalDateTime matriculadaEm = LocalDateTime.now();
    private LocalDateTime concluidoEm;
}