package com.techmentor.api.dto;

import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class OportunidadeDtos {
    private OportunidadeDtos() { }
    public record OportunidadeRequest(@NotNull Oportunidade.Tipo tipo,
                                      @NotBlank @Size(max = 255) String titulo,
                                      @Size(max = 255) String organizacao,
                                      @Size(max = 2000) String descricao,
                                      @Size(max = 2000) String requisitos,
                                      @Size(max = 255) String localidade,
                                      @Size(max = 255) String linkExterno,
                                      LocalDate prazoInscricao) { }
    public record OportunidadeResponse(Long id, Oportunidade.Tipo tipo, String titulo, String organizacao,
                                       String descricao, String requisitos, String localidade, String linkExterno,
                                       LocalDate prazoInscricao, boolean ativa) { }
    public record InscricaoResponse(Long id, Long usuarioId, Long oportunidadeId, String titulo,
                                    InscricaoOportunidade.Status status, LocalDateTime criadaEm) { }
}
