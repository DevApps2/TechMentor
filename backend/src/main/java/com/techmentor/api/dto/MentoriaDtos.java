package com.techmentor.api.dto;

import com.techmentor.api.model.Mentoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

public final class MentoriaDtos {
    private MentoriaDtos() { }

    @Schema(description = "Dados para criar uma área de atuação")
    public record AreaRequest(
            @Schema(description = "Nome da área de atuação", example = "Back-end")
            @NotBlank @Size(max = 80) String nome) { }

    @Schema(description = "Área de atuação de mentoras")
    public record AreaResponse(
            @Schema(description = "ID da área de atuação", example = "1")
            Long id,
            @Schema(description = "Nome da área de atuação", example = "Back-end")
            String nome) { }

    @Schema(description = "Dados para criar ou atualizar o perfil de mentora")
    public record MentoraRequest(
            @Schema(description = "Biografia da mentora", example = "Engenheira de software apaixonada por ensinar")
            @Size(max = 2000) String bio,
            @Schema(description = "Cargo atual", example = "Engenheira de Software Sênior")
            @Size(max = 255) String cargo,
            @Schema(description = "Empresa atual", example = "Acme Tecnologia")
            @Size(max = 255) String empresa,
            @Schema(description = "Anos de experiência profissional", example = "8")
            @PositiveOrZero Integer anosExperiencia,
            @Schema(description = "IDs das áreas de atuação da mentora", example = "[1, 2]")
            Set<@Positive Long> areasIds) { }

    @Schema(description = "Perfil de mentora")
    public record MentoraResponse(
            @Schema(description = "ID do perfil de mentora", example = "1")
            Long id,
            @Schema(description = "ID do usuário da mentora", example = "3")
            Long usuarioId,
            @Schema(description = "Nome da mentora", example = "Maria Silva")
            String nome,
            @Schema(description = "URL da foto da mentora", example = "https://cdn.techmentor.com/fotos/maria.png")
            String fotoUrl,
            @Schema(description = "Biografia da mentora", example = "Engenheira de software apaixonada por ensinar")
            String bio,
            @Schema(description = "Cargo atual", example = "Engenheira de Software Sênior")
            String cargo,
            @Schema(description = "Empresa atual", example = "Acme Tecnologia")
            String empresa,
            @Schema(description = "Anos de experiência profissional", example = "8")
            int anosExperiencia,
            @Schema(description = "Nota média das avaliações recebidas", example = "4.8")
            double notaMedia,
            @Schema(description = "Total de avaliações recebidas", example = "25")
            int totalAvaliacoes,
            @Schema(description = "Nomes das áreas de atuação da mentora", example = "[\"Back-end\", \"Carreira\"]")
            Set<String> areas) { }

    @Schema(description = "Dados para cadastrar um horário de disponibilidade da mentora")
    public record HorarioRequest(
            @Schema(description = "Início do horário disponível", example = "2026-11-10T14:00:00")
            @NotNull LocalDateTime inicio,
            @Schema(description = "Fim do horário disponível", example = "2026-11-10T15:00:00")
            @NotNull LocalDateTime fim) { }

    @Schema(description = "Horário de disponibilidade de uma mentora")
    public record HorarioResponse(
            @Schema(description = "ID do horário", example = "1")
            Long id,
            @Schema(description = "ID da mentora", example = "1")
            Long mentoraId,
            @Schema(description = "Início do horário disponível", example = "2026-11-10T14:00:00")
            LocalDateTime inicio,
            @Schema(description = "Fim do horário disponível", example = "2026-11-10T15:00:00")
            LocalDateTime fim,
            @Schema(description = "Indica se o horário já foi reservado em um agendamento", example = "false")
            boolean reservado) { }

    @Schema(description = "Dados para agendar uma mentoria")
    public record AgendamentoRequest(
            @Schema(description = "ID do horário de disponibilidade escolhido", example = "1")
            @NotNull @Positive Long horarioId,
            @Schema(description = "Assunto que a aluna deseja tratar na mentoria", example = "Dúvidas sobre transição de carreira")
            @Size(max = 500) String assunto) { }

    @Schema(description = "Agendamento de mentoria entre aluna e mentora")
    public record AgendamentoResponse(
            @Schema(description = "ID do agendamento", example = "1")
            Long id,
            @Schema(description = "ID da aluna", example = "2")
            Long alunaId,
            @Schema(description = "Nome da aluna", example = "Ana Souza")
            String alunaNome,
            @Schema(description = "ID da mentora", example = "1")
            Long mentoraId,
            @Schema(description = "Nome da mentora", example = "Maria Silva")
            String mentoraNome,
            @Schema(description = "ID do horário reservado", example = "1")
            Long horarioId,
            @Schema(description = "Início da mentoria", example = "2026-11-10T14:00:00")
            LocalDateTime inicio,
            @Schema(description = "Fim da mentoria", example = "2026-11-10T15:00:00")
            LocalDateTime fim,
            @Schema(description = "Situação do agendamento", example = "AGENDADA")
            Mentoria.Status status,
            @Schema(description = "Assunto da mentoria", example = "Dúvidas sobre transição de carreira")
            String assunto,
            @Schema(description = "Data e hora em que o agendamento foi criado", example = "2026-10-26T14:30:00")
            LocalDateTime criadaEm) { }
}
