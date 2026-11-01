package com.techmentor.api.evento;

import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.Usuario;
import java.time.LocalDateTime;

/** Fatos do sistema que geram uma notificação para alguém. Quem publica não sabe quem escuta. */
public final class EventosDeNotificacao {
    private EventosDeNotificacao() { }

    public record ConquistaDesbloqueada(Usuario usuario, String nomeDaConquista) { }

    public record NivelSubiu(Usuario usuario, int nivel) { }

    public record CursoConcluido(Usuario usuario, String tituloDoCurso) { }

    public record DesafioConcluido(Usuario usuario, String tituloDoDesafio) { }

    // nomeDoDenunciado é nulo em denúncias gerais (sem pessoa indicada)
    public record StatusDaDenunciaMudou(Usuario denunciante, String nomeDoDenunciado, Denuncia.Status novoStatus) { }

    // A aluna pediu uma mentoria: a mentora precisa confirmar
    public record MentoriaAgendada(Usuario mentora, String nomeDaAluna, LocalDateTime inicio) { }

    // A mentora confirmou: avisa a aluna
    public record MentoriaConfirmada(Usuario aluna, String nomeDaMentora, LocalDateTime inicio) { }

    // Alguém cancelou: avisa a outra pessoa (destinatario) dizendo quem cancelou
    public record MentoriaCancelada(Usuario destinatario, String quemCancelou, LocalDateTime inicio) { }

    // Alguém trocou o horário: avisa a outra pessoa. precisaConfirmar = a mentora ainda vai confirmar o novo horário
    public record MentoriaReagendada(Usuario destinatario, String quemReagendou, LocalDateTime novoInicio, boolean precisaConfirmar) { }
}