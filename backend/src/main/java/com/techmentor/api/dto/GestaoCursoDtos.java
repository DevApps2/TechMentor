package com.techmentor.api.dto;

import com.techmentor.api.model.SituacaoCurso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTOs da gestão de cursos pela administradora (endpoints por id, sob /api/admin). */
public final class GestaoCursoDtos {
    private GestaoCursoDtos() { }

    @Schema(description = "Dados de um módulo. A posição não é informada: o módulo novo entra no fim, e a ordem muda pelo endpoint de reordenar")
    public record ModuloGestaoRequest(
            @Schema(description = "Título do módulo", example = "Introdução ao Java")
            @NotBlank @Size(max = 150) String titulo) { }

    @Schema(description = "Dados de uma aula. A posição não é informada: a aula nova entra no fim do módulo")
    public record AulaGestaoRequest(
            @Schema(description = "Título da aula", example = "História do Java")
            @NotBlank @Size(max = 200) String titulo,
            @Schema(description = "Duração da aula em minutos", example = "36")
            @PositiveOrZero int duracaoMinutos,
            @Schema(description = "XP concedido ao concluir a aula; se omitido, usa o valor padrão", example = "20")
            @PositiveOrZero Integer xpRecompensa,
            @Schema(description = "Link do vídeo: YouTube (watch?v=ID ou youtu.be/ID) ou arquivo .mp4 em https",
                    example = "https://www.youtube.com/watch?v=sTX0UEplF54")
            @Size(max = 255)
            @Pattern(regexp = LinksDeVideo.PADRAO, message = LinksDeVideo.MENSAGEM)
            String conteudoUrl) { }

    @Schema(description = "Nova situação do curso")
    public record SituacaoRequest(
            @Schema(description = "RASCUNHO, PUBLICADO ou ARQUIVADO", example = "PUBLICADO")
            @NotNull SituacaoCurso situacao) { }

    @Schema(description = "A nova ordem: os ids de todos os itens, na sequência desejada")
    public record OrdemRequest(
            @Schema(description = "Ids na nova ordem; precisa conter exatamente os itens atuais, cada um uma vez", example = "[12, 10, 11]")
            @NotNull @NotEmpty List<Long> ids) { }

    @Schema(description = "Quantas alunas concluíram uma aula")
    public record AulaMetrica(
            Long aulaId,
            @Schema(description = "Posição do módulo no curso", example = "1") int ordemModulo,
            @Schema(description = "Posição da aula no módulo", example = "2") int ordemAula,
            String titulo,
            @Schema(description = "Alunas diferentes que concluíram esta aula", example = "14") long concluiram) { }

    @Schema(description = "Números de um curso para a administradora. Nunca identifica alunas")
    public record MetricasCursoResponse(
            Long cursoId,
            String titulo,
            SituacaoCurso situacao,
            @Schema(description = "Alunas matriculadas", example = "30") long matriculas,
            @Schema(description = "Alunas que concluíram pelo menos uma aula", example = "22") long alunasComProgresso,
            @Schema(description = "Alunas que concluíram o curso inteiro", example = "9") long concluintes,
            @Schema(description = "Percentual de matriculadas que concluíram (0 a 100)", example = "30") int taxaConclusao,
            @Schema(description = "Quantas alunas concluíram cada aula, na ordem do curso") List<AulaMetrica> aulas,
            @Schema(description = "A aula depois da qual mais alunas pararam; nulo se ninguém parou", nullable = true)
            AulaMetrica aulaOndeMaisPararam,
            @Schema(description = "Quantas alunas pararam nessa aula", example = "5") long alunasQuePararam) { }
}
