package com.techmentor.api.dto;

import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class OportunidadeDtos {
    private OportunidadeDtos() { }

    @Schema(description = "Dados para criar ou atualizar uma oportunidade")
    public record OportunidadeRequest(
            @Schema(description = "Tipo da oportunidade", example = "ESTAGIO")
            @NotNull Oportunidade.Tipo tipo,
            @Schema(description = "Título da oportunidade", example = "Estágio em Desenvolvimento Java")
            @NotBlank @Size(max = 255) String titulo,
            @Schema(description = "Organização responsável", example = "Acme Tecnologia")
            @Size(max = 255) String organizacao,
            @Schema(description = "Descrição da oportunidade", example = "Atuação no time de back-end com Spring Boot")
            @Size(max = 2000) String descricao,
            @Schema(description = "Requisitos para participar", example = "Cursando Ciência da Computação ou áreas afins")
            @Size(max = 2000) String requisitos,
            @Schema(description = "Localidade ou modalidade", example = "São Paulo - SP (híbrido)")
            @Size(max = 255) String localidade,
            @Schema(description = "Link externo para mais informações ou inscrição", example = "https://acme.com/carreiras/estagio-java")
            @Size(max = 255) String linkExterno,
            @Schema(description = "Data limite para inscrição", example = "2026-12-15")
            LocalDate prazoInscricao) { }

    @Schema(description = "Oportunidade, vaga ou evento publicado na plataforma")
    public record OportunidadeResponse(
            @Schema(description = "ID da oportunidade", example = "1")
            Long id,
            @Schema(description = "Tipo da oportunidade", example = "ESTAGIO")
            Oportunidade.Tipo tipo,
            @Schema(description = "Título da oportunidade", example = "Estágio em Desenvolvimento Java")
            String titulo,
            @Schema(description = "Organização responsável", example = "Acme Tecnologia")
            String organizacao,
            @Schema(description = "Descrição da oportunidade", example = "Atuação no time de back-end com Spring Boot")
            String descricao,
            @Schema(description = "Requisitos para participar", example = "Cursando Ciência da Computação ou áreas afins")
            String requisitos,
            @Schema(description = "Localidade ou modalidade", example = "São Paulo - SP (híbrido)")
            String localidade,
            @Schema(description = "Link externo para mais informações ou inscrição", example = "https://acme.com/carreiras/estagio-java")
            String linkExterno,
            @Schema(description = "Data limite para inscrição", example = "2026-12-15")
            LocalDate prazoInscricao,
            @Schema(description = "Indica se a oportunidade ainda está ativa", example = "true")
            boolean ativa) { }

    @Schema(description = "Candidatura ou inscrição de uma usuária em uma oportunidade")
    public record InscricaoResponse(
            @Schema(description = "ID da candidatura/inscrição", example = "1")
            Long id,
            @Schema(description = "ID da usuária", example = "2")
            Long usuarioId,
            @Schema(description = "ID da oportunidade", example = "1")
            Long oportunidadeId,
            @Schema(description = "Título da oportunidade", example = "Estágio em Desenvolvimento Java")
            String titulo,
            @Schema(description = "Situação da candidatura/inscrição", example = "ENVIADA")
            InscricaoOportunidade.Status status,
            @Schema(description = "Data e hora em que a candidatura/inscrição foi criada", example = "2026-10-26T14:30:00")
            LocalDateTime criadaEm) { }
}
