package com.techmentor.api.evento;

import com.techmentor.api.evento.EventosDeNotificacao.ConquistaDesbloqueada;
import com.techmentor.api.evento.EventosDeNotificacao.CursoConcluido;
import com.techmentor.api.evento.EventosDeNotificacao.DesafioConcluido;
import com.techmentor.api.evento.EventosDeNotificacao.NivelSubiu;
import com.techmentor.api.evento.EventosDeNotificacao.StatusDaDenunciaMudou;
import com.techmentor.api.model.DestinoNotificacao;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.TipoNotificacao;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.service.NivelRegraService;
import com.techmentor.api.service.NotificacaoService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaAgendada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaCancelada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaConfirmada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaReagendada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaConcluida;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeMentoria;
import com.techmentor.api.evento.EventosDeNotificacao.AvaliacaoRecebida;
import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeAvaliacao;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaExpirada;
import com.techmentor.api.evento.EventosDeNotificacao.CandidaturaAtualizada;
import com.techmentor.api.evento.EventosDeNotificacao.CursoEnviadoParaRevisao;
import com.techmentor.api.evento.EventosDeNotificacao.CursoRevisado;
import com.techmentor.api.evento.EventosDeNotificacao.ParceiraAceitouConvite;
import com.techmentor.api.evento.EventosDeNotificacao.SituacaoDaParceiraMudou;

/**
 * Observer das notificações: escuta os fatos do sistema e cria a notificação de cada um.
 * É um @EventListener comum (síncrono), então roda na mesma transação de quem publicou:
 * a ação e a notificação são gravadas juntas, ou nenhuma das duas.
 */
@Component
public class NotificadorListener {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoService notificacoes;

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    // Ex.: "10/11 às 14:00"
    private static final DateTimeFormatter DATA_E_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    public NotificadorListener(NotificacaoService notificacoes) {
        this.notificacoes = notificacoes;
    }

    @EventListener
    public void aoDesbloquearConquista(ConquistaDesbloqueada evento) {
        notificacoes.notificar(evento.usuario(), TipoNotificacao.CONQUISTA, DestinoNotificacao.CONQUISTAS, null, "Conquista desbloqueada!", evento.nomeDaConquista());
    }

    @EventListener
    public void aoSubirDeNivel(NivelSubiu evento) {
        notificacoes.notificar(evento.usuario(), TipoNotificacao.NIVEL, DestinoNotificacao.CONQUISTAS, null, "Você subiu de nível!",
                "Agora você é nível " + evento.nivel() + " · " + NivelRegraService.titulo(evento.nivel()));
    }

    @EventListener
    public void aoConcluirCurso(CursoConcluido evento) {
        notificacoes.notificar(evento.usuario(), TipoNotificacao.CURSO, DestinoNotificacao.CURSO, evento.tituloDoCurso(), "Curso concluído!", evento.tituloDoCurso());
    }

    @EventListener
    public void aoConcluirDesafio(DesafioConcluido evento) {
        notificacoes.notificar(evento.usuario(), TipoNotificacao.DESAFIO, DestinoNotificacao.DESAFIOS, null, "Desafio concluído!", evento.tituloDoDesafio());
    }

    @EventListener
    public void aoMudarStatusDaDenuncia(StatusDaDenunciaMudou evento) {

        String sobre = evento.nomeDoDenunciado() == null ? "" : " sobre " + evento.nomeDoDenunciado();

        String mensagem = switch (evento.novoStatus()) {
            case EM_ANALISE -> "Sua denúncia" + sobre + " está em análise pela equipe.";
            case RESOLVIDA -> "Sua denúncia" + sobre + " foi resolvida. Obrigada por ajudar a manter o TechMentor seguro.";
            case ABERTA -> "Sua denúncia" + sobre + " foi reaberta.";
        };

        notificacoes.notificar(evento.denunciante(), TipoNotificacao.DENUNCIA, DestinoNotificacao.DENUNCIAS, null, "Atualização da sua denúncia", mensagem);
    }

    @EventListener
    public void aoAgendarMentoria(MentoriaAgendada evento) {
        notificacoes.notificar(evento.mentora(), TipoNotificacao.MENTORIA, DestinoNotificacao.MENTORIAS, null, "Nova mentoria para confirmar",
                evento.nomeDaAluna() + " quer conversar em " + evento.inicio().format(DATA_E_HORA) + ".");
    }

    @EventListener
    public void aoConfirmarMentoria(MentoriaConfirmada evento) {
        notificacoes.notificar(evento.aluna(), TipoNotificacao.MENTORIA, DestinoNotificacao.MENTORIAS, null, "Mentoria confirmada!",
                evento.nomeDaMentora() + " confirmou a mentoria de " + evento.inicio().format(DATA_E_HORA) + ".");
    }

    @EventListener
    public void aoCancelarMentoria(MentoriaCancelada evento) {
        notificacoes.notificar(evento.destinatario(), TipoNotificacao.MENTORIA, DestinoNotificacao.MENTORIAS, null, "Mentoria cancelada",
                evento.quemCancelou() + " cancelou a mentoria de " + evento.inicio().format(DATA_E_HORA) + ".");
    }

    /**
     * Chamado pelo relógio (não é rota): a mentoria ficou AGENDADA e o horário começou sem a mentora confirmar.
     * Cancela e avisa as duas pessoas. O horário continua reservado de propósito: ele já passou,
     * e liberar faria um horário antigo aparecer como disponível no perfil da mentora.
     */
    @EventListener
    public void aoExpirarMentoria(MentoriaExpirada evento) {
        String quando = evento.inicio().format(DATA_E_HORA);

        if (evento.destinatarioEhMentora()) {
            notificacoes.notificar(evento.destinatario(), TipoNotificacao.MENTORIA, DestinoNotificacao.MENTORIAS, null, "Mentoria expirada",
                    "A mentoria de " + quando + " com " + evento.outraPessoa()
                            + " não foi confirmada a tempo e foi cancelada.");
        } else {
            notificacoes.notificar(evento.destinatario(), TipoNotificacao.MENTORIA, DestinoNotificacao.MENTORIAS, null, "Mentoria não confirmada",
                    "A mentoria de " + quando + " com " + evento.outraPessoa()
                            + " não foi confirmada a tempo e foi cancelada. Que tal escolher outro horário?");
        }
    }

    @EventListener
    public void aoReagendarMentoria(MentoriaReagendada evento) {
        String complemento = evento.precisaConfirmar()
                ? " Abra o app para confirmar o novo horário."
                : " Se não puder, cancele ou escolha outro horário.";

        notificacoes.notificar(evento.destinatario(), TipoNotificacao.MENTORIA, DestinoNotificacao.MENTORIAS, null, "Mentoria reagendada",
                evento.quemReagendou() + " mudou a mentoria para " + evento.novoInicio().format(DATA_E_HORA) + "." + complemento);
    }

    @EventListener
    public void aoLembrarMentoria(LembreteDeMentoria evento) {
        notificacoes.notificar(evento.destinatario(), TipoNotificacao.MENTORIA, DestinoNotificacao.MENTORIAS, null, "Sua mentoria começa em breve",
                "Mentoria com " + evento.outraPessoa() + " em " + evento.inicio().format(DATA_E_HORA)
                        + ". A sala já está aberta no app.");
    }

    @EventListener
    public void aoConcluirMentoria(MentoriaConcluida evento) {
        notificacoes.notificar(evento.destinatario(), TipoNotificacao.AVALIACAO, DestinoNotificacao.AVALIACOES, null, "Como foi sua mentoria?",
                "Avalie sua mentoria com " + evento.outraPessoa() + " (" + evento.inicio().format(DATA_E_HORA) + ").");
    }

    @EventListener
    public void aoLembrarDeAvaliar(LembreteDeAvaliacao evento) {
        notificacoes.notificar(evento.destinatario(), TipoNotificacao.AVALIACAO, DestinoNotificacao.AVALIACOES, null, "Falta avaliar sua mentoria",
                "Sua opinião sobre a mentoria com " + evento.outraPessoa() + " vale até " + evento.prazoFinal().format(DIA) + ".");
    }

    @EventListener
    public void aoReceberAvaliacao(AvaliacaoRecebida evento) {
        notificacoes.notificar(evento.destinatario(), TipoNotificacao.AVALIACAO, DestinoNotificacao.AVALIACOES, null, "Você recebeu uma avaliação",
                evento.quemAvaliou() + " avaliou a mentoria com " + evento.nota()
                        + (evento.nota() == 1 ? " estrela." : " estrelas."));
    }

    @EventListener
    public void aoAtualizarCandidatura(CandidaturaAtualizada evento) {
        String o = "\"" + evento.tituloDaOportunidade() + "\"";
        String tipo = evento.evento() ? "inscrição" : "candidatura";

        String mensagem = switch (evento.novoStatus()) {
            case EM_ANALISE -> "Sua " + tipo + " em " + o + " está em análise pela equipe.";
            case APROVADA -> evento.evento()
                    ? "Sua inscrição em " + o + " foi confirmada!"
                    : "Boa notícia! Sua candidatura em " + o + " foi aprovada.";
            case RECUSADA -> evento.evento()
                    ? "Sua inscrição em " + o + " não pôde ser confirmada."
                    : "Sua candidatura em " + o + " não seguiu adiante desta vez. Continue tentando!";
            case ENVIADA -> "Sua " + tipo + " em " + o + " voltou para a fila de análise.";
        };

        notificacoes.notificar(evento.destinatario(), TipoNotificacao.OPORTUNIDADE, DestinoNotificacao.OPORTUNIDADES, null, "Atualização da sua " + tipo, mensagem);
    }

    // ---- Parcerias ----

    @EventListener
    public void aoParceiraAceitarConvite(ParceiraAceitouConvite evento) {
        notificacoes.notificarAdministradoras(TipoNotificacao.PARCERIA, DestinoNotificacao.PARCEIRA_PARA_VERIFICAR,
                String.valueOf(evento.parceiraId()), "Parceira aguardando verificação",
                evento.nomeDaInstituicao() + " aceitou o convite e os termos. Falta conferir o cadastro para liberar a criação de cursos.");
    }

    @EventListener
    public void aoMudarSituacaoDaParceira(SituacaoDaParceiraMudou evento) {
        String titulo;
        String mensagem;
        switch (evento.nova()) {
            case ATIVA -> {
                titulo = "Sua conta foi verificada";
                mensagem = "Tudo certo! Você já pode criar cursos e enviá-los para revisão.";
            }
            case SUSPENSA -> {
                titulo = "Sua conta de parceira foi suspensa";
                mensagem = "Motivo: " + evento.motivo();
            }
            default -> {
                titulo = "Atualização da sua parceria";
                mensagem = "A situação da sua conta mudou.";
            }
        }
        notificacoes.notificar(evento.parceira(), TipoNotificacao.PARCERIA, DestinoNotificacao.PERFIL_DA_PARCEIRA, null,
                titulo, mensagem);
    }

    @EventListener
    public void aoEnviarCursoParaRevisao(CursoEnviadoParaRevisao evento) {
        notificacoes.notificarAdministradoras(TipoNotificacao.REVISAO, DestinoNotificacao.REVISAO_DE_CURSO,
                String.valueOf(evento.cursoId()), "Curso aguardando revisão",
                evento.nomeDaParceira() + " enviou \"" + evento.tituloDoCurso() + "\" para análise.");
    }

    @EventListener
    public void aoRevisarCurso(CursoRevisado evento) {
        String o = "\"" + evento.tituloDoCurso() + "\"";
        String titulo;
        String mensagem;
        switch (evento.decisao()) {
            case APROVADO -> {
                titulo = "Seu curso foi publicado!";
                mensagem = o + " foi aprovado e já aparece para as alunas.";
            }
            case DEVOLVIDO -> {
                titulo = "Seu curso foi devolvido";
                mensagem = o + " precisa de ajustes: " + evento.comentario();
            }
            default -> {
                titulo = "Seu curso foi recusado";
                mensagem = o + " não pôde ser publicado: " + evento.comentario();
            }
        }
        notificacoes.notificar(evento.parceira(), TipoNotificacao.PARCERIA, DestinoNotificacao.CURSO_DA_PARCEIRA,
                String.valueOf(evento.cursoId()), titulo, mensagem);
    }
}
