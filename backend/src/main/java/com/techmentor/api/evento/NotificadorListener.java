package com.techmentor.api.evento;

import com.techmentor.api.evento.EventosDeNotificacao.ConquistaDesbloqueada;
import com.techmentor.api.evento.EventosDeNotificacao.CursoConcluido;
import com.techmentor.api.evento.EventosDeNotificacao.DesafioConcluido;
import com.techmentor.api.evento.EventosDeNotificacao.NivelSubiu;
import com.techmentor.api.evento.EventosDeNotificacao.StatusDaDenunciaMudou;
import com.techmentor.api.service.NivelRegraService;
import com.techmentor.api.service.NotificacaoService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer das notificações: escuta os fatos do sistema e cria a notificação de cada um.
 * É um @EventListener comum (síncrono), então roda na mesma transação de quem publicou:
 * a ação e a notificação são gravadas juntas, ou nenhuma das duas.
 */
@Component
public class NotificadorListener {

    private final NotificacaoService notificacoes;

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
}
