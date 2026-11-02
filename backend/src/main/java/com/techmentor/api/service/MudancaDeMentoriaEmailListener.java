package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaCancelada;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaReagendada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** E-mails de cancelamento e reagendamento. Mesmas regras dos outros: depois de gravar, em outra thread, falha só vira log. */
@Component
public class MudancaDeMentoriaEmailListener {

    private static final Logger log = LoggerFactory.getLogger(MudancaDeMentoriaEmailListener.class);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    // Ex.: "segunda-feira, 02/11 às 14:00"
    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", PT_BR);
    // Ex.: "02/11 às 14:00" (para o assunto do e-mail)
    private static final DateTimeFormatter CURTO = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final EmailService emailService;

    public MudancaDeMentoriaEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoCancelar(MentoriaCancelada evento) {
        try {
            ModeloDeEmail modelo = ModeloDeEmail.com("Mentoria cancelada")
                    .saudacao("Olá, " + evento.nomeDoDestinatario() + "!")
                    .paragrafo(evento.quemCancelou() + " cancelou a mentoria que aconteceria em:")
                    .detalhe("Data e hora", quando(evento.inicio()))
                    .paragrafo("O horário foi liberado. Se quiser, abra o aplicativo do TechMentor e marque uma nova conversa.");

            emailService.enviar(
                    evento.emailDoDestinatario(),
                    "Mentoria cancelada (" + evento.inicio().format(CURTO) + ") - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o e-mail de cancelamento da mentoria: {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoReagendar(MentoriaReagendada evento) {
        try {
            ModeloDeEmail modelo = ModeloDeEmail.com("Mentoria reagendada")
                    .saudacao("Olá, " + evento.nomeDoDestinatario() + "!")
                    .paragrafo(evento.quemReagendou() + " mudou o horário da mentoria. O novo horário é:")
                    .detalhe("Nova data e hora", quando(evento.novoInicio()));

            if (evento.precisaConfirmar()) {
                modelo.paragrafo("Abra o aplicativo do TechMentor para confirmar o novo horário.");
            } else {
                modelo.paragrafo("Se não puder nesse horário, abra o aplicativo do TechMentor para escolher outro ou cancelar.")
                        .paragrafo("A sala do Google Meet continua valendo para o novo horário: o link chega por e-mail 30 minutos antes.");
            }

            emailService.enviar(
                    evento.emailDoDestinatario(),
                    "Mentoria reagendada para " + evento.novoInicio().format(CURTO) + " - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o e-mail de reagendamento da mentoria: {}", e.getMessage());
        }
    }

    private static String quando(LocalDateTime data) {
        String texto = data.format(QUANDO);
        return texto.substring(0, 1).toUpperCase(PT_BR) + texto.substring(1);
    }
}