package com.techmentor.api.dto;

import com.techmentor.api.model.Denuncia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class SegurancaDtos {
    private SegurancaDtos() { }

    @Schema(description = "Dados para registrar uma denúncia")
    public record DenunciaRequest(
            @Schema(description = "ID do usuário denunciado; opcional em denúncias gerais", example = "5", nullable = true)
            @Positive Long usuarioDenunciadoId,
            @Schema(description = "Motivo da denúncia", example = "ASSEDIO")
            @NotNull Denuncia.Motivo motivo,
            @Schema(description = "Detalhes da denúncia", example = "Mensagens ofensivas enviadas no chat")
            @Size(max = 2000) String descricao) { }

    @Schema(description = "Denúncia registrada na plataforma")
    public record DenunciaResponse(
            @Schema(description = "ID da denúncia", example = "1")
            Long id,
            @Schema(description = "ID de quem denunciou", example = "2")
            Long denuncianteId,
            @Schema(description = "ID do usuário denunciado; nulo em denúncias gerais", example = "5", nullable = true)
            Long denunciadoId,
            @Schema(description = "Motivo da denúncia", example = "ASSEDIO")
            Denuncia.Motivo motivo,
            @Schema(description = "Detalhes da denúncia", example = "Mensagens ofensivas enviadas no chat")
            String descricao,
            @Schema(description = "Situação da denúncia", example = "ABERTA")
            Denuncia.Status status,
            @Schema(description = "Data e hora em que a denúncia foi criada", example = "2026-10-26T14:30:00")
            LocalDateTime criadaEm) { }

    @Schema(description = "Dados para bloquear um usuário")
    public record BloqueioRequest(
            @Schema(description = "ID do usuário a ser bloqueado", example = "5")
            @NotNull @Positive Long usuarioId) { }

    @Schema(description = "Usuário bloqueado")
    public record BloqueioResponse(
            @Schema(description = "ID do usuário bloqueado", example = "5")
            Long usuarioId,
            @Schema(description = "Nome do usuário bloqueado", example = "Carlos Lima")
            String nome,
            @Schema(description = "Data e hora do bloqueio", example = "2026-10-26T14:30:00")
            LocalDateTime criadoEm) { }
}
