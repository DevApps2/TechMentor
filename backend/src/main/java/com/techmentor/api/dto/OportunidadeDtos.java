package com.techmentor.api.dto;

import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class OportunidadeDtos {
    private OportunidadeDtos() { }
    public record OportunidadeRequest(Oportunidade.Tipo tipo, String titulo, String organizacao, String descricao,
                                      String requisitos, String localidade, String linkExterno, LocalDate prazoInscricao) { }
    public record OportunidadeResponse(Long id, Oportunidade.Tipo tipo, String titulo, String organizacao,
                                       String descricao, String requisitos, String localidade, String linkExterno,
                                       LocalDate prazoInscricao, boolean ativa) { }
    public record InscricaoResponse(Long id, Long usuarioId, Long oportunidadeId, String titulo,
                                    InscricaoOportunidade.Status status, LocalDateTime criadaEm) { }
}
