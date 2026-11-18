package com.techmentor.api.evento;

import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.Usuario;
import java.time.LocalDateTime;
import java.util.UUID;

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

    // ---- Parcerias ----

    // A administradora convidou uma parceira. O código em texto só existe aqui, na memória, para o e-mail.
    public record ParceiraConvidada(String nomeDaInstituicao, String responsavel, String email, String codigo,
                                    LocalDateTime expiraEm, UUID eventoId) {
        public ParceiraConvidada(String nomeDaInstituicao, String responsavel, String email, String codigo, LocalDateTime expiraEm) {
            this(nomeDaInstituicao, responsavel, email, codigo, expiraEm, UUID.randomUUID());
        }
    }

    // A parceira criou a senha e aceitou os termos: falta a verificação da administradora
    public record ParceiraAceitouConvite(Long parceiraId, String nomeDaInstituicao, String email, UUID eventoId) {
        public ParceiraAceitouConvite(Long parceiraId, String nomeDaInstituicao) {
            this(parceiraId, nomeDaInstituicao, null, UUID.randomUUID());
        }
    }

    // A administradora verificou, suspendeu ou reativou a parceira. motivo só vem na suspensão.
    public record SituacaoDaParceiraMudou(Usuario parceira, SituacaoParceira nova, String motivo, UUID eventoId) {
        public SituacaoDaParceiraMudou(Usuario parceira, SituacaoParceira nova, String motivo) {
            this(parceira, nova, motivo, UUID.randomUUID());
        }
    }

    // A parceira terminou o curso e pediu a análise
    public record CursoEnviadoParaRevisao(Long cursoId, String tituloDoCurso, String nomeDaParceira, UUID eventoId) {
        public CursoEnviadoParaRevisao(Long cursoId, String tituloDoCurso, String nomeDaParceira) {
            this(cursoId, tituloDoCurso, nomeDaParceira, UUID.randomUUID());
        }
    }

    public enum DecisaoDeRevisao { APROVADO, DEVOLVIDO, RECUSADO }

    // A administradora decidiu. comentario vem na devolução e na recusa.
    public record CursoRevisado(Usuario parceira, Long cursoId, String tituloDoCurso, DecisaoDeRevisao decisao,
                                String comentario, UUID eventoId) {
        public CursoRevisado(Usuario parceira, Long cursoId, String tituloDoCurso, DecisaoDeRevisao decisao,
                             String comentario) {
            this(parceira, cursoId, tituloDoCurso, decisao, comentario, UUID.randomUUID());
        }
    }
}
