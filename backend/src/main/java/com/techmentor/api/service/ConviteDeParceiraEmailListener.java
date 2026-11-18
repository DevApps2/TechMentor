package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.ParceiraConvidada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

/** E-mail do convite para a parceira. Mesmas regras dos outros: depois de gravar, em outra thread, falha só vira log. */
@Component
public class ConviteDeParceiraEmailListener {

    private static final Logger log = LoggerFactory.getLogger(ConviteDeParceiraEmailListener.class);
    private static final DateTimeFormatter PRAZO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;

    public ConviteDeParceiraEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoConvidar(ParceiraConvidada evento) {
        try {
            ModeloDeEmail modelo = ModeloDeEmail.com("Convite de parceria")
                    .saudacao("Olá, " + evento.responsavel() + "!")
                    .paragrafo(evento.nomeDaInstituicao() + " foi convidada para cadastrar cursos no TechMentor. "
                            + "Para criar a sua senha e aceitar os Termos de Parceria, use o código abaixo no aplicativo:")
                    .destaque("Seu código de convite", evento.codigo())
                    .paragrafo("O código vale até " + evento.expiraEm().format(PRAZO) + " e só pode ser usado uma vez. "
                            + "Depois do cadastro, a nossa equipe confere os dados da instituição e libera a criação de cursos.")
                    .nota("Se você não esperava este convite, ignore este e-mail. "
                            + "Nunca compartilhe este código com ninguém.");

            emailService.enviarRastreado("parceira-convite:" + evento.eventoId(), evento.email(),
                    "Convite de parceria - TechMentor", modelo, false);
        } catch (Exception e) {
            log.warn("Não foi possível preparar o convite de parceria ({}).", e.getClass().getSimpleName());
        }
    }
}
