package com.techmentor.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

/** Agendamento de uma conversa entre aluna e mentora (a mentora vem de horario.getMentora()). */
@Entity @Table(name = "mentorias")
@Getter @Setter @NoArgsConstructor
public class Mentoria {
    @Schema(description = "Situação do agendamento de mentoria: AGENDADA, CONFIRMADA, CONCLUIDA ou CANCELADA")
    public enum Status { AGENDADA, CONFIRMADA, CONCLUIDA, CANCELADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluna_id")
    private Usuario aluna;

    // Um mesmo horário pode voltar a ser usado depois que o agendamento é cancelado.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id")
    private HorarioDisponivel horario;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.AGENDADA;
    @Column(length = 500) private String assunto;
    @Column(updatable = false) private LocalDateTime criadaEm = LocalDateTime.now();
}
