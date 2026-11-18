package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.PedidoDeParceriaRecebido;
import com.techmentor.api.evento.EventosDeNotificacao.PedidoDeParceriaRecusado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * E-mails do pedido de parceria: a confirmação para a instituição, o aviso para a equipe e a resposta de recusa.
 * (Na aprovação, quem avisa é o próprio e-mail de convite.) Mesmas regras dos outros: depois de gravar, em outra thread,
 * e a falha do e-mail nunca desfaz o pedido.
 */
@Component
public class PedidoDeParceriaEmailListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoDeParceriaEmailListener.class);

    private final EmailService emailService;

    // O e-mail da equipe TechMentor: recebe o aviso de cada pedido e aparece como contato nas respostas
    @Value("${app.pedidos.email:}")
    private String emailDaEquipe;

    public PedidoDeParceriaEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoReceberPedido(PedidoDeParceriaRecebido evento) {
        // 1) confirmação para quem pediu
        ModeloDeEmail confirmacao = ModeloDeEmail.com("Recebemos o seu pedido")
                .saudacao("Olá, " + evento.responsavel() + "!")
                .paragrafo("Recebemos o pedido de parceria de " + evento.nomeDaInstituicao()
                        + ". Nossa equipe vai ler com atenção e responder neste e-mail.")
                .detalhe("Instituição", evento.nomeDaInstituicao())
                .detalhe("E-mail de contato", evento.email())
                .paragrafo("Se o pedido for aprovado, você recebe outra mensagem, com um link para criar a sua senha e entrar "
                        + "na área das parceiras." + contatoDaEquipe())
                .nota("Você recebeu esta mensagem porque este e-mail foi informado no formulário \"Seja nosso parceiro\" "
                        + "do TechMentor. Se não foi você, pode ignorá-la.");
        enviar("pedido-recebido:" + evento.eventoId(), evento.email(), "Recebemos o seu pedido de parceria - TechMentor", confirmacao);

        // 2) aviso para a equipe
        if (emailDaEquipe == null || emailDaEquipe.isBlank()) return;
        ModeloDeEmail aviso = ModeloDeEmail.com("Novo pedido de parceria")
                .saudacao("Olá, equipe!")
                .paragrafo("Chegou um pedido de parceria pelo site.")
                .detalhe("Instituição", evento.nomeDaInstituicao())
                .detalhe("Tipo", evento.tipo())
                .detalhe("Responsável", evento.responsavel())
                .detalhe("E-mail", evento.email());
        if (evento.site() != null && !evento.site().isBlank()) aviso.detalhe("Site", evento.site());
        aviso.detalhe("O que pretende oferecer", evento.mensagem())
                .paragrafo("Entre no app como administradora para analisar. O aviso também aparece no sininho.");
        enviar("pedido-equipe:" + evento.eventoId(), emailDaEquipe, "Novo pedido de parceria: " + evento.nomeDaInstituicao(), aviso);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoRecusarPedido(PedidoDeParceriaRecusado evento) {
        ModeloDeEmail resposta = ModeloDeEmail.com("Sobre o seu pedido de parceria")
                .saudacao("Olá, " + evento.responsavel() + "!")
                .paragrafo("Analisamos o pedido de parceria de " + evento.nomeDaInstituicao()
                        + " e, por enquanto, não vamos seguir com ele.")
                .destaque("Motivo", evento.motivo())
                .paragrafo("Se quiser conversar ou enviar um novo pedido com ajustes, a porta continua aberta." + contatoDaEquipe());
        enviar("pedido-recusado:" + evento.eventoId(), evento.email(), "Sobre o seu pedido de parceria - TechMentor", resposta);
    }

    private String contatoDaEquipe() {
        return emailDaEquipe == null || emailDaEquipe.isBlank() ? "" : " Qualquer dúvida, escreva para " + emailDaEquipe + ".";
    }

    private void enviar(String chave, String destinatario, String assunto, ModeloDeEmail modelo) {
        try {
            emailService.enviarRastreado(chave, destinatario, assunto, modelo, true);
        } catch (Exception e) {
            log.warn("Não foi possível preparar o e-mail do pedido de parceria {} ({}).", chave, e.getClass().getSimpleName());
        }
    }
}
