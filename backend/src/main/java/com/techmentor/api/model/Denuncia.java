package com.techmentor.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "denuncias")
@Getter @Setter @NoArgsConstructor
public class Denuncia {
    @Schema(description = "Motivo da denúncia: ASSEDIO, COMPORTAMENTO_INADEQUADO, SPAM (spam ou fraude), DISCURSO_DE_ODIO, PERFIL_FALSO ou OUTRO")
    public enum Motivo { ASSEDIO, DISCURSO_DE_ODIO, SPAM, PERFIL_FALSO, OUTRO, COMPORTAMENTO_INADEQUADO }
    @Schema(description = "Situação da denúncia: ABERTA, EM_ANALISE ou RESOLVIDA")
    public enum Status { ABERTA, EM_ANALISE, RESOLVIDA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario denunciante;
    @ManyToOne(fetch = FetchType.LAZY) private Usuario denunciado; // pode ser nulo (denúncia geral)

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Motivo motivo;
    @Column(length = 2000) private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.ABERTA;
    private LocalDateTime criadaEm = LocalDateTime.now();
}
