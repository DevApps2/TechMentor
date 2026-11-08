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
    public record MentoriaAgendada(Usuario mentora, String nomeDaAluna, LocalDateTime inicio,
                                   String nomeDaMentora, String emailDaMentora, String assunto) { }

    // linkSala pode ser nulo se o Google não devolveu a sala na hora.
    public record MentoriaConfirmada(Usuario aluna, String nomeDaMentora, LocalDateTime inicio,
                                     LocalDateTime fim, String linkSala,
                                     String nomeDaAluna, String emailDaAluna, String assunto) { }

    // Alguém cancelou: avisa a outra pessoa (destinatario) dizendo quem cancelou.
    // Nome e e-mail em texto simples são para o e-mail, que sai em outra thread.
    public record MentoriaCancelada(Usuario destinatario, String quemCancelou, LocalDateTime inicio,
                                    String nomeDoDestinatario, String emailDoDestinatario) { }

    // Ninguém confirmou a tempo: a mentoria foi cancelada pelo sistema. Um evento por pessoa.
    // outraPessoa = o nome de quem estava do outro lado; destinatarioEhMentora muda o texto do aviso.
    public record MentoriaExpirada(Usuario destinatario, String outraPessoa, LocalDateTime inicio,
                                   boolean destinatarioEhMentora) { }

    // Alguém trocou o horário: avisa a outra pessoa. precisaConfirmar = a mentora ainda vai confirmar o novo horário
    public record MentoriaReagendada(Usuario destinatario, String quemReagendou, LocalDateTime novoInicio, boolean precisaConfirmar,
                                     String nomeDoDestinatario, String emailDoDestinatario) { }

    // Falta pouco para a mentoria: um evento por pessoa. linkSala pode ser nulo se o Google não devolveu a sala
    public record LembreteDeMentoria(
            Usuario destinatario,
            String nomeDoDestinatario,
            String emailDoDestinatario,
            String outraPessoa,
            boolean destinatarioEhMentora,
            LocalDateTime inicio,
            LocalDateTime fim,
            LocalDateTime salaAbreEm,
            String linkSala,
            String assunto) { }

    // A mentoria terminou: convida a pessoa a avaliar. prazoFinal = até quando dá. Nome e e-mail em texto simples são para o e-mail.
    public record MentoriaConcluida(Usuario destinatario, String nomeDoDestinatario, String emailDoDestinatario,
                                    String outraPessoa, boolean destinatarioEhMentora,
                                    LocalDateTime inicio, LocalDateTime prazoFinal) { }

    // Passaram alguns dias e a pessoa ainda não avaliou
    public record LembreteDeAvaliacao(Usuario destinatario, String nomeDoDestinatario, String emailDoDestinatario,
                                      String outraPessoa, boolean destinatarioEhMentora,
                                      LocalDateTime inicio, LocalDateTime prazoFinal) { }

    // Alguém avaliou esta pessoa. quemAvaliou já vem no formato certo (abreviado quando a aluna avalia a mentora).
    // destinatarioEhMentora = quem recebeu foi a mentora (avaliação pública); senão foi a aluna (feedback privado).
    public record AvaliacaoRecebida(Usuario destinatario, String nomeDoDestinatario, String emailDoDestinatario,
                                    String quemAvaliou, boolean destinatarioEhMentora, int nota, String comentario) { }

    // A equipe mudou a situacao de uma candidatura (ou inscricao em evento): avisa quem se candidatou
    public record CandidaturaAtualizada(Usuario destinatario, String tituloDaOportunidade, boolean evento,
                                        com.techmentor.api.model.InscricaoOportunidade.Status novoStatus) { }
}
