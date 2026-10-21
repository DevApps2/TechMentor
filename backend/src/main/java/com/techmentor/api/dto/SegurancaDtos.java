package com.techmentor.api.dto;

import com.techmentor.api.model.Denuncia;
import java.time.LocalDateTime;

public final class SegurancaDtos {
    private SegurancaDtos() { }
    public record DenunciaRequest(Long usuarioDenunciadoId, Denuncia.Motivo motivo, String descricao) { }
    public record DenunciaResponse(Long id, Long denuncianteId, Long denunciadoId, Denuncia.Motivo motivo,
                                   String descricao, Denuncia.Status status, LocalDateTime criadaEm) { }
    public record BloqueioRequest(Long usuarioId) { }
    public record BloqueioResponse(Long usuarioId, String nome, LocalDateTime criadoEm) { }
}
