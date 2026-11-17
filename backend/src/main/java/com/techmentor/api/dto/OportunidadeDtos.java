package com.techmentor.api.dto;

import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
            LocalDate prazoInscricao,
            @Schema(description = "Benefícios oferecidos", example = "Mentoria e horário flexível")
            @Size(max = 2000) String beneficios,
            @Schema(description = "Área da oportunidade", example = "Programação")
            @Size(max = 80) String area,
            @Schema(description = "Nível esperado (texto livre)", example = "Iniciante")
            @Size(max = 50) String nivel) { }

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
            boolean ativa,
            @Schema(description = "Benefícios oferecidos", example = "Mentoria e horário flexível", nullable = true)
            String beneficios,
            @Schema(description = "Área da oportunidade", example = "Programação", nullable = true)
            String area,
            @Schema(description = "Nível esperado", example = "Iniciante", nullable = true)
            String nivel) { }

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

    @Schema(description = "Dados que a candidata preenche ao se candidar a uma vaga ou bolsa. Corpo opcional: sem ele a candidatura é só registrada")
    public record CandidaturaRequest(
            @Schema(description = "Nome completo", example = "Camila Rocha")
            @NotBlank(message = "Informe o nome") @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres") String nome,
            @Schema(description = "E-mail de contato", example = "camila.rocha@email.com")
            @NotBlank(message = "Informe o e-mail") @jakarta.validation.constraints.Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres") String email,
            @Schema(description = "Telefone com DDD", example = "(11) 99999-9999")
            @NotBlank(message = "Informe o telefone")
            @Pattern(regexp = "^\\D*(\\d\\D*){10,11}$", message = "Informe o telefone com DDD") String telefone,
            @Schema(description = "Cidade e estado", example = "São Paulo, SP", nullable = true)
            @Size(max = 120, message = "A cidade deve ter no máximo 120 caracteres") String cidade,
            @Schema(description = "LinkedIn ou portfólio (http:// ou https://)", example = "https://linkedin.com/in/camila", nullable = true)
            @Size(max = 255, message = "O link deve ter no máximo 255 caracteres")
            @Pattern(regexp = "^$|^https?://\\S+$", message = "O link deve começar com http:// ou https://") String linkProfissional,
            @Schema(description = "Apresentação da candidata", nullable = true)
            @Size(max = 500, message = "A mensagem deve ter no máximo 500 caracteres") String mensagem) { }

    @Schema(description = "Candidatura vista pela administração, com os dados de quem se candidatou")
    public record CandidaturaAdminResponse(
            @Schema(description = "ID da candidatura/inscrição", example = "1")
            Long id,
            @Schema(description = "ID da usuária", example = "2")
            Long usuarioId,
            @Schema(description = "Nome da usuária", example = "Camila Rocha")
            String usuarioNome,
            @Schema(description = "E-mail da usuária", example = "camila.rocha@email.com")
            String usuarioEmail,
            @Schema(description = "ID da oportunidade", example = "1")
            Long oportunidadeId,
            @Schema(description = "Título da oportunidade", example = "Estágio em Desenvolvimento Java")
            String titulo,
            @Schema(description = "Situação: ENVIADA, EM_ANALISE, APROVADA ou RECUSADA", example = "ENVIADA")
            InscricaoOportunidade.Status status,
            @Schema(description = "Quando a usuária se candidatou", example = "2026-10-26T14:30:00")
            LocalDateTime criadaEm,
            @Schema(description = "Nome informado no formulário; nulo em eventos e candidaturas antigas", nullable = true) String contatoNome,
            @Schema(description = "E-mail de contato informado no formulário", nullable = true) String contatoEmail,
            @Schema(description = "Telefone informado no formulário", nullable = true) String telefone,
            @Schema(description = "Cidade e estado", nullable = true) String cidade,
            @Schema(description = "LinkedIn ou portfólio", nullable = true) String linkProfissional,
            @Schema(description = "Apresentação da candidata", nullable = true) String mensagem,
            @Schema(description = "Se a candidata anexou o currículo em PDF") boolean temCurriculo,
            @Schema(description = "Nome original do PDF", nullable = true) String nomeDoCurriculo) { }

    @Schema(description = "Nova situação de uma candidatura (só administradores)")
    public record CandidaturaStatusRequest(
            @Schema(description = "ENVIADA, EM_ANALISE, APROVADA ou RECUSADA", example = "EM_ANALISE")
            @NotNull(message = "Informe a nova situação") InscricaoOportunidade.Status status) { }
}
