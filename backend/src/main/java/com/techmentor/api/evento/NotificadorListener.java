package com.techmentor.api.evento;

import com.techmentor.api.evento.EventosDeNotificacao.ConquistaDesbloqueada;
import com.techmentor.api.evento.EventosDeNotificacao.CursoConcluido;
import com.techmentor.api.evento.EventosDeNotificacao.DesafioConcluido;
import com.techmentor.api.evento.EventosDeNotificacao.NivelSubiu;
import com.techmentor.api.evento.EventosDeNotificacao.StatusDaDenunciaMudou;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.service.NivelRegraService;
import com.techmentor.api.service.NotificacaoService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaAgendada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaCancelada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaConfirmada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaReagendada;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeMentoria;

/**
 * Observer das notificações: escuta os fatos do sistema e cria a notificação de cada um.
 * É um @EventListener comum (síncrono), então roda na mesma transação de quem publicou:
 * a ação e a notificação são gravadas juntas, ou nenhuma das duas.
 */
@Component
public class NotificadorListener {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoService notificacoes;
    // Ex.: "10/11 às 14:00"
    private static final DateTimeFormatter DATA_E_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    public NotificadorListener(NotificacaoService notificacoes) {
        this.notificacoes = notificacoes;
    }

    @EventListener
    public void aoDesbloquearConquista(ConquistaDesbloqueada evento) {
        notificacoes.notificar(evento.usuario(), "Conquista desbloqueada!", evento.nomeDaConquista());
    }

    @EventListener
    public void aoSubirDeNivel(NivelSubiu evento) {
        notificacoes.notificar(evento.usuario(), "Você subiu de nível!",
                "Agora você é nível " + evento.nivel() + " · " + NivelRegraService.titulo(evento.nivel()));
    }

    @EventListener
    public void aoConcluirCurso(CursoConcluido evento) {
        notificacoes.notificar(evento.usuario(), "Curso concluído!", evento.tituloDoCurso());
    }

    @EventListener
    public void aoConcluirDesafio(DesafioConcluido evento) {
        notificacoes.notificar(evento.usuario(), "Desafio concluído!", evento.tituloDoDesafio());
    }

    @EventListener
    public void aoMudarStatusDaDenuncia(StatusDaDenunciaMudou evento) {

        String sobre = evento.nomeDoDenunciado() == null ? "" : " sobre " + evento.nomeDoDenunciado();

        String mensagem = switch (evento.novoStatus()) {
            case EM_ANALISE -> "Sua denúncia" + sobre + " está em análise pela equipe.";
            case RESOLVIDA -> "Sua denúncia" + sobre + " foi resolvida. Obrigada por ajudar a manter o TechMentor seguro.";
            case ABERTA -> "Sua denúncia" + sobre + " foi reaberta.";
        };

        notificacoes.notificar(evento.denunciante(), "Atualização da sua denúncia", mensagem);
    }

    @EventListener
    public void aoAgendarMentoria(MentoriaAgendada evento) {
        notificacoes.notificar(evento.mentora(), "Nova mentoria para confirmar",
                evento.nomeDaAluna() + " quer conversar em " + evento.inicio().format(DATA_E_HORA) + ".");
    }

    @EventListener
    public void aoConfirmarMentoria(MentoriaConfirmada evento) {
        notificacoes.notificar(evento.aluna(), "Mentoria confirmada!",
                evento.nomeDaMentora() + " confirmou a mentoria de " + evento.inicio().format(DATA_E_HORA) + ".");
    }

    @EventListener
    public void aoCancelarMentoria(MentoriaCancelada evento) {
        notificacoes.notificar(evento.destinatario(), "Mentoria cancelada",
                evento.quemCancelou() + " cancelou a mentoria de " + evento.inicio().format(DATA_E_HORA) + ".");
    }

    @EventListener
    public void aoReagendarMentoria(MentoriaReagendada evento) {
        String complemento = evento.precisaConfirmar()
                ? " Abra o app para confirmar o novo horário."
                : " Se não puder, cancele ou escolha outro horário.";

        notificacoes.notificar(evento.destinatario(), "Mentoria reagendada",
                evento.quemReagendou() + " mudou a mentoria para " + evento.novoInicio().format(DATA_E_HORA) + "." + complemento);
    }

    @EventListener
    public void aoLembrarMentoria(LembreteDeMentoria evento) {
        notificacoes.notificar(evento.destinatario(), "Sua mentoria começa em breve",
                "Mentoria com " + evento.outraPessoa() + " em " + evento.inicio().format(DATA_E_HORA)
                        + ". A sala já está aberta no app.");
    }
}
