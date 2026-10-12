package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "inscricoes_oportunidade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "oportunidade_id"}))
@Getter @Setter @NoArgsConstructor
public class InscricaoOportunidade {
    public enum Status { ENVIADA, EM_ANALISE, APROVADA, RECUSADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Oportunidade oportunidade;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.ENVIADA;
    private LocalDateTime criadaEm = LocalDateTime.now();
}
