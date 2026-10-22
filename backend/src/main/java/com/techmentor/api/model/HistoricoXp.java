package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_xp")
@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"id", "pontos", "motivo", "criadoEm"})
public class HistoricoXp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;

    private int pontos;
    private String motivo;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
