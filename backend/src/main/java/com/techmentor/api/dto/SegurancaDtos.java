package com.techmentor.api.dto;

import com.techmentor.api.model.Denuncia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class SegurancaDtos {
    private SegurancaDtos() { }
    public record DenunciaRequest(@Positive Long usuarioDenunciadoId,
                                  @NotNull Denuncia.Motivo motivo,
                                  @Size(max = 2000) String descricao) { }
    public record DenunciaResponse(Long id, Long denuncianteId, Long denunciadoId, Denuncia.Motivo motivo,
                                   String descricao, Denuncia.Status status, LocalDateTime criadaEm) { }
    public record BloqueioRequest(@NotNull @Positive Long usuarioId) { }
    public record BloqueioResponse(Long usuarioId, String nome, LocalDateTime criadoEm) { }
}
