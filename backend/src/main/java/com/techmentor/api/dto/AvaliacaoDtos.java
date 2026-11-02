package com.techmentor.api.dto;

import com.techmentor.api.model.Avaliacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AvaliacaoDtos {
    private AvaliacaoDtos() { }

    @Schema(description = "Avaliação de uma mentoria concluída")
    public record AvaliacaoRequest(
            @Schema(description = "Nota de 1 a 5 estrelas", example = "5")
            @NotNull(message = "A nota é obrigatória")
            @Min(value = 1, message = "A nota deve ser de 1 a 5")
            @Max(value = 5, message = "A nota deve ser de 1 a 5") Integer nota,
            @Schema(description = "Comentário opcional", example = "Conversa muito útil, saí com um plano claro.", nullable = true)
            @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres") String comentario) { }

    @Schema(description = "Avaliação registrada")
    public record AvaliacaoResponse(
            @Schema(description = "ID da avaliação", example = "1")
            Long id,
            @Schema(description = "ID da mentoria avaliada", example = "5")
            Long mentoriaId,
            @Schema(description = "Quem escreveu: ALUNA ou MENTORA", example = "ALUNA")
            Avaliacao.Autor autor,
            @Schema(description = "Nome de quem avaliou", example = "Daniela Martins")
            String avaliadorNome,
            @Schema(description = "Nota de 1 a 5", example = "5")
            int nota,
            @Schema(description = "Comentário; nulo se a pessoa não escreveu", nullable = true)
            String comentario,
            @Schema(description = "Quando foi avaliada", example = "2026-11-03T15:20:00")
            LocalDateTime criadaEm) { }

    @Schema(description = "Situação da avaliação de uma mentoria para quem está perguntando")
    public record AvaliacaoDaMentoriaResponse(
            @Schema(description = "ID da mentoria", example = "5")
            Long mentoriaId,
            @Schema(description = "Se esta pessoa pode avaliar agora", example = "true")
            boolean podeAvaliar,
            @Schema(description = "Por que não pode avaliar; nulo se pode", nullable = true,
                    example = "Você já avaliou esta mentoria.")
            String motivoDeNaoPoder,
            @Schema(description = "Até quando dá para avaliar", example = "2026-11-17T15:00:00")
            LocalDateTime prazoFinal,
            @Schema(description = "O que esta pessoa avaliou; nulo se ainda não avaliou", nullable = true)
            AvaliacaoResponse minhaAvaliacao,
            @Schema(description = "O que a outra pessoa escreveu sobre esta; nulo se ainda não escreveu", nullable = true)
            AvaliacaoResponse recebida) { }

    @Schema(description = "Mentoria concluída que a pessoa ainda pode avaliar")
    public record AvaliacaoPendenteResponse(
            @Schema(description = "ID da mentoria", example = "5")
            Long mentoriaId,
            @Schema(description = "O papel de quem pergunta nesta mentoria: ALUNA ou MENTORA", example = "ALUNA")
            Avaliacao.Autor voce,
            @Schema(description = "Nome da outra pessoa, a que será avaliada", example = "Maria Silva")
            String outraPessoaNome,
            @Schema(description = "Foto da outra pessoa", nullable = true)
            String outraPessoaFotoUrl,
            @Schema(description = "Início da mentoria", example = "2026-11-03T14:00:00")
            LocalDateTime inicio,
            @Schema(description = "Até quando dá para avaliar", example = "2026-11-17T15:00:00")
            LocalDateTime prazoFinal) { }

    @Schema(description = "Avaliação pública de uma mentora; o nome da aluna vem abreviado")
    public record AvaliacaoPublicaResponse(
            @Schema(description = "ID da avaliação", example = "1")
            Long id,
            @Schema(description = "Quem avaliou, abreviado", example = "Daniela M.")
            String avaliadora,
            @Schema(description = "Nota de 1 a 5", example = "5")
            int nota,
            @Schema(description = "Comentário; nulo se a aluna não escreveu", nullable = true)
            String comentario,
            @Schema(description = "Quando foi avaliada", example = "2026-11-03T15:20:00")
            LocalDateTime criadaEm) { }

    @Schema(description = "Nota média, distribuição por estrela e comentários de uma mentora")
    public record AvaliacoesDaMentoraResponse(
            @Schema(description = "Nota média", example = "4.5")
            double notaMedia,
            @Schema(description = "Total de avaliações", example = "2")
            int totalAvaliacoes,
            @Schema(description = "Quantas avaliações de cada nota (de 5 a 1)", example = "{\"5\": 1, \"4\": 1, \"3\": 0, \"2\": 0, \"1\": 0}")
            Map<Integer, Long> distribuicao,
            @Schema(description = "As avaliações mais recentes, sem as ocultadas pela moderação")
            List<AvaliacaoPublicaResponse> avaliacoes) { }
}