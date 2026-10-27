package com.techmentor.api.dto;

import com.techmentor.api.model.UsuarioDesafio;
import com.techmentor.api.model.Conquista;
import com.techmentor.api.model.Desafio;
import com.techmentor.api.model.HistoricoXp;
import com.techmentor.api.model.Notificacao;
import com.techmentor.api.model.ProgressoAula;
import com.techmentor.api.model.UsuarioConquista;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** DTOs usados pelos endpoints de conquistas, desafios, notificações e progresso. */
public final class GamificacaoDtos {
    private GamificacaoDtos() { }

    @Schema(description = "Dados para criar ou atualizar uma conquista")
    public record ConquistaRequest(
            @Schema(description = "Nome da conquista", example = "Primeira aula")
            @NotBlank String nome,
            @Schema(description = "Descrição da conquista", example = "Concluiu a primeira aula na plataforma")
            @Size(max = 500) String descricao,
            @Schema(description = "URL do ícone da conquista", example = "https://cdn.techmentor.com/conquistas/primeira-aula.png")
            @Size(max = 255) String iconeUrl,
            @Schema(description = "XP bônus concedido ao desbloquear a conquista", example = "50")
            @PositiveOrZero int xpBonus
    ) {
        public Conquista toEntity() {
            Conquista conquista = new Conquista();
            conquista.setNome(nome);
            conquista.setDescricao(descricao);
            conquista.setIconeUrl(iconeUrl);
            conquista.setXpBonus(xpBonus);
            return conquista;
        }
    }

    @Schema(description = "Conquista cadastrada na plataforma")
    public record ConquistaResponse(
            @Schema(description = "ID da conquista", example = "1")
            Long id,
            @Schema(description = "Nome da conquista", example = "Primeira aula")
            String nome,
            @Schema(description = "Descrição da conquista", example = "Concluiu a primeira aula na plataforma")
            String descricao,
            @Schema(description = "URL do ícone da conquista", example = "https://cdn.techmentor.com/conquistas/primeira-aula.png")
            String iconeUrl,
            @Schema(description = "XP bônus concedido ao desbloquear a conquista", example = "50")
            int xpBonus
    ) {
        public static ConquistaResponse from(Conquista conquista) {
            return new ConquistaResponse(conquista.getId(), conquista.getNome(), conquista.getDescricao(),
                    conquista.getIconeUrl(), conquista.getXpBonus());
        }
    }

    @Schema(description = "Conquista desbloqueada por uma usuária")
    public record UsuarioConquistaResponse(
            @Schema(description = "ID do registro de conquista desbloqueada", example = "1")
            Long id,
            @Schema(description = "ID da usuária", example = "1")
            Long usuarioId,
            @Schema(description = "ID da conquista", example = "1")
            Long conquistaId,
            @Schema(description = "Nome da conquista", example = "Primeira aula")
            String nomeConquista,
            @Schema(description = "Data e hora em que a conquista foi desbloqueada", example = "2026-10-26T14:30:00")
            LocalDateTime desbloqueadaEm
    ) {
        public static UsuarioConquistaResponse from(UsuarioConquista item) {
            return new UsuarioConquistaResponse(item.getId(), item.getUsuario().getId(),
                    item.getConquista().getId(), item.getConquista().getNome(), item.getDesbloqueadaEm());
        }
    }

    @Schema(description = "Dados para criar ou atualizar um desafio")
    public record DesafioRequest(
            @Schema(description = "Título do desafio", example = "Maratona de Java")
            @NotBlank String titulo,
            @Schema(description = "Descrição do desafio", example = "Conclua 5 aulas de Java durante a semana")
            @Size(max = 1000) String descricao,
            @Schema(description = "XP concedido ao concluir o desafio", example = "100")
            @PositiveOrZero int xpRecompensa,
            @Schema(description = "Data de início do desafio", example = "2026-11-01")
            @NotNull LocalDate inicio,
            @Schema(description = "Data de término do desafio; deve ser igual ou posterior ao início", example = "2026-11-07")
            @NotNull LocalDate fim,
            @Schema(description = "Quantidade de aulas a concluir para vencer o desafio", example = "5")
            @Positive int metaAulas
    ) {
        @AssertTrue(message = "A data.sql final deve ser igual ou posterior à data.sql inicial")
        public boolean periodoValido() {
            return inicio == null || fim == null || !fim.isBefore(inicio);
        }

        public Desafio toEntity() {
            Desafio desafio = new Desafio();
            desafio.setTitulo(titulo);
            desafio.setDescricao(descricao);
            desafio.setXpRecompensa(xpRecompensa);
            desafio.setInicio(inicio);
            desafio.setFim(fim);
            desafio.setMetaAulas(metaAulas);
            return desafio;
        }
    }

    @Schema(description = "Desafio cadastrado na plataforma")
    public record DesafioResponse(
            @Schema(description = "ID do desafio", example = "1")
            Long id,
            @Schema(description = "Título do desafio", example = "Maratona de Java")
            String titulo,
            @Schema(description = "Descrição do desafio", example = "Conclua 5 aulas de Java durante a semana")
            String descricao,
            @Schema(description = "XP concedido ao concluir o desafio", example = "100")
            int xpRecompensa,
            @Schema(description = "Data de início do desafio", example = "2026-11-01")
            LocalDate inicio,
            @Schema(description = "Data de término do desafio", example = "2026-11-07")
            LocalDate fim,
            @Schema(description = "Quantidade de aulas a concluir para vencer o desafio", example = "5")
            int metaAulas
    ) {
        public static DesafioResponse from(Desafio desafio) {
            return new DesafioResponse(desafio.getId(), desafio.getTitulo(), desafio.getDescricao(),
                    desafio.getXpRecompensa(), desafio.getInicio(), desafio.getFim(), desafio.getMetaAulas());
        }
    }

    @Schema(description = "Participação de uma usuária em um desafio")
    public record UsuarioDesafioResponse(
            @Schema(description = "ID da participação", example = "1")
            Long id,
            @Schema(description = "ID da usuária", example = "1")
            Long usuarioId,
            @Schema(description = "ID do desafio", example = "1")
            Long desafioId,
            @Schema(description = "Título do desafio", example = "Maratona de Java")
            String tituloDesafio,
            @Schema(description = "Situação da participação", example = "EM_ANDAMENTO")
            UsuarioDesafio.Status status,
            @Schema(description = "Aulas concluídas pela usuária dentro do período do desafio", example = "3")
            Long aulasConcluidas,
            @Schema(description = "Data e hora da conclusão do desafio; nulo enquanto não concluído", example = "2026-11-05T20:15:00", nullable = true)
            LocalDateTime concluidoEm
    ) {
        public static UsuarioDesafioResponse from(UsuarioDesafio item) {
            return new UsuarioDesafioResponse(item.getId(), item.getUsuario().getId(),
                    item.getDesafio().getId(), item.getDesafio().getTitulo(), item.getStatus(),
                    item.getAulasConcluidas(), item.getConcluidoEm());
        }
    }

    @Schema(description = "Notificação enviada a uma usuária")
    public record NotificacaoResponse(
            @Schema(description = "ID da notificação", example = "1")
            Long id,
            @Schema(description = "ID da usuária destinatária", example = "1")
            Long usuarioId,
            @Schema(description = "Título da notificação", example = "Nova conquista!")
            String titulo,
            @Schema(description = "Mensagem da notificação", example = "Você desbloqueou a conquista Primeira aula")
            String mensagem,
            @Schema(description = "Indica se a notificação já foi lida", example = "false")
            boolean lida,
            @Schema(description = "Data e hora de criação da notificação", example = "2026-10-26T14:30:00")
            LocalDateTime criadaEm
    ) {
        public static NotificacaoResponse from(Notificacao notificacao) {
            return new NotificacaoResponse(notificacao.getId(), notificacao.getUsuario().getId(),
                    notificacao.getTitulo(), notificacao.getMensagem(), notificacao.isLida(),
                    notificacao.getCriadaEm());
        }
    }

    @Schema(description = "Registro de XP ganho por uma usuária")
    public record HistoricoXpResponse(
            @Schema(description = "ID do registro", example = "1")
            Long id,
            @Schema(description = "ID da usuária", example = "1")
            Long usuarioId,
            @Schema(description = "Pontos de XP do registro", example = "10")
            int pontos,
            @Schema(description = "Motivo pelo qual o XP foi concedido", example = "Aula concluída: Variáveis e tipos de dados")
            String motivo,
            @Schema(description = "Data e hora do registro", example = "2026-10-26T14:30:00")
            LocalDateTime criadoEm
    ) {
        public static HistoricoXpResponse from(HistoricoXp historico) {
            return new HistoricoXpResponse(historico.getId(), historico.getUsuario().getId(),
                    historico.getPontos(), historico.getMotivo(), historico.getCriadoEm());
        }
    }

    @Schema(description = "Aula concluída por uma usuária")
    public record ProgressoAulaResponse(
            @Schema(description = "ID do registro de progresso", example = "1")
            Long id,
            @Schema(description = "ID da usuária", example = "1")
            Long usuarioId,
            @Schema(description = "ID da aula", example = "1")
            Long aulaId,
            @Schema(description = "Título da aula", example = "Variáveis e tipos de dados")
            String tituloAula,
            @Schema(description = "Data e hora da conclusão da aula", example = "2026-10-26T14:30:00")
            LocalDateTime concluidaEm
    ) {
        public static ProgressoAulaResponse from(ProgressoAula progresso) {
            return new ProgressoAulaResponse(progresso.getId(), progresso.getUsuario().getId(),
                    progresso.getAula().getId(), progresso.getAula().getTitulo(), progresso.getConcluidaEm());
        }
    }

    @Schema(description = "Progresso de uma usuária em um curso")
    public record ProgressoCursoResponse(
            @Schema(description = "Aulas do curso já concluídas pela usuária", example = "4")
            long aulasConcluidas,
            @Schema(description = "Total de aulas do curso", example = "12")
            long totalAulas
    ) { }
}
