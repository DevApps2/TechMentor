package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "horarios_disponiveis")
@Getter @Setter @NoArgsConstructor
public class HorarioDisponivel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentora_id")
    private PerfilMentora mentora;

    @Column(nullable = false) private LocalDateTime inicio;
    @Column(nullable = false) private LocalDateTime fim;
    private boolean reservado = false;
}
