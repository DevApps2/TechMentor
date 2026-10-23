package com.techmentor.api.dto;

import com.techmentor.api.model.UsuarioDesafio;
import com.techmentor.api.model.Conquista;
import com.techmentor.api.model.Desafio;
import com.techmentor.api.model.HistoricoXp;
import com.techmentor.api.model.Notificacao;
import com.techmentor.api.model.ProgressoAula;
import com.techmentor.api.model.UsuarioConquista;
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

    public record ConquistaRequest(
            @NotBlank String nome,
            @Size(max = 500) String descricao,
            @Size(max = 255) String iconeUrl,
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

    public record ConquistaResponse(
            Long id,
            String nome,
            String descricao,
            String iconeUrl,
            int xpBonus
    ) {
        public static ConquistaResponse from(Conquista conquista) {
            return new ConquistaResponse(conquista.getId(), conquista.getNome(), conquista.getDescricao(),
                    conquista.getIconeUrl(), conquista.getXpBonus());
        }
    }

    public record UsuarioConquistaResponse(
            Long id,
            Long usuarioId,
            Long conquistaId,
            String nomeConquista,
            LocalDateTime desbloqueadaEm
    ) {
        public static UsuarioConquistaResponse from(UsuarioConquista item) {
            return new UsuarioConquistaResponse(item.getId(), item.getUsuario().getId(),
                    item.getConquista().getId(), item.getConquista().getNome(), item.getDesbloqueadaEm());
        }
    }

    public record DesafioRequest(
            @NotBlank String titulo,
            @Size(max = 1000) String descricao,
            @PositiveOrZero int xpRecompensa,
            @NotNull LocalDate inicio,
            @NotNull LocalDate fim,
            @Positive int metaAulas
    ) {
        @AssertTrue(message = "A data final deve ser igual ou posterior à data inicial")
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

    public record DesafioResponse(
            Long id,
            String titulo,
            String descricao,
            int xpRecompensa,
            LocalDate inicio,
            LocalDate fim,
            int metaAulas
    ) {
        public static DesafioResponse from(Desafio desafio) {
            return new DesafioResponse(desafio.getId(), desafio.getTitulo(), desafio.getDescricao(),
                    desafio.getXpRecompensa(), desafio.getInicio(), desafio.getFim(), desafio.getMetaAulas());
        }
    }

    public record UsuarioDesafioResponse(
            Long id,
            Long usuarioId,
            Long desafioId,
            String tituloDesafio,
            UsuarioDesafio.Status status,
            Long aulasConcluidas,
            LocalDateTime concluidoEm
    ) {
        public static UsuarioDesafioResponse from(UsuarioDesafio item) {
            return new UsuarioDesafioResponse(item.getId(), item.getUsuario().getId(),
                    item.getDesafio().getId(), item.getDesafio().getTitulo(), item.getStatus(),
                    item.getAulasConcluidas(), item.getConcluidoEm());
        }
    }

    public record NotificacaoResponse(
            Long id,
            Long usuarioId,
            String titulo,
            String mensagem,
            boolean lida,
            LocalDateTime criadaEm
    ) {
        public static NotificacaoResponse from(Notificacao notificacao) {
            return new NotificacaoResponse(notificacao.getId(), notificacao.getUsuario().getId(),
                    notificacao.getTitulo(), notificacao.getMensagem(), notificacao.isLida(),
                    notificacao.getCriadaEm());
        }
    }

    public record HistoricoXpResponse(
            Long id,
            Long usuarioId,
            int pontos,
            String motivo,
            LocalDateTime criadoEm
    ) {
        public static HistoricoXpResponse from(HistoricoXp historico) {
            return new HistoricoXpResponse(historico.getId(), historico.getUsuario().getId(),
                    historico.getPontos(), historico.getMotivo(), historico.getCriadoEm());
        }
    }

    public record ProgressoAulaResponse(
            Long id,
            Long usuarioId,
            Long aulaId,
            String tituloAula,
            LocalDateTime concluidaEm
    ) {
        public static ProgressoAulaResponse from(ProgressoAula progresso) {
            return new ProgressoAulaResponse(progresso.getId(), progresso.getUsuario().getId(),
                    progresso.getAula().getId(), progresso.getAula().getTitulo(), progresso.getConcluidaEm());
        }
    }

    public record ProgressoCursoResponse(
            long aulasConcluidas,
            long totalAulas
    ) { }
}
