package com.techmentor.api.dto;

import com.techmentor.api.model.AcaoAuditada;
import com.techmentor.api.model.RegistroAuditoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** O histórico de quem fez o quê. */
public final class AuditoriaDtos {
    private AuditoriaDtos() { }

    @Schema(description = "Uma linha do histórico de um curso ou de uma parceira")
    public record RegistroResponse(
            Long id,
            @Schema(description = "Quando aconteceu") LocalDateTime quando,
            @Schema(description = "Quem fez; nulo quando foi o próprio sistema", nullable = true) Long autorId,
            @Schema(description = "Nome de quem fez, como era na hora", nullable = true, example = "Administradora TechMentor") String autorNome,
            @Schema(description = "ADMIN, PARCEIRA...", nullable = true, example = "ADMIN") String autorPapel,
            @Schema(description = "O que foi feito", example = "CURSO_PUBLICADO") AcaoAuditada acao,
            @Schema(description = "A mesma ação, em português, pronta para mostrar", example = "Curso publicado") String descricao,
            @Schema(description = "Detalhe da ação (o que mudou, o comentário...)", nullable = true) String detalhe) {

        public static RegistroResponse from(RegistroAuditoria r) {
            return new RegistroResponse(r.getId(), r.getQuando(), r.getAutorId(), r.getAutorNome(), r.getAutorPapel(),
                    r.getAcao(), r.getAcao().descricao(), r.getDetalhe());
        }
    }
}
