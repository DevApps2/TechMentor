package com.techmentor.api.model;

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
    public enum Status { AGENDADA, CONFIRMADA, CONCLUIDA, CANCELADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluna_id")
    private Usuario aluna;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", unique = true)
    private HorarioDisponivel horario;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.AGENDADA;
    @Column(length = 500) private String assunto;
    @Column(updatable = false) private LocalDateTime criadaEm = LocalDateTime.now();
}
