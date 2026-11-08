package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaAgendada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** E-mail para a mentora quando uma aluna pede um horário. Mesmas regras dos outros: depois de gravar, em outra thread, falha só vira log. */
@Component
public class NovaMentoriaEmailListener {

    private static final Logger log = LoggerFactory.getLogger(NovaMentoriaEmailListener.class);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    // Ex.: "segunda-feira, 02/11 às 14:00"
    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", PT_BR);
    // Ex.: "02/11 às 14:00" (para o assunto do e-mail)
    private static final DateTimeFormatter CURTO = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final EmailService emailService;

    public NovaMentoriaEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoSolicitar(MentoriaAgendada evento) {
        try {
            ModeloDeEmail modelo = ModeloDeEmail.com("Nova solicitação de mentoria")
                    .saudacao("Olá, " + evento.nomeDaMentora() + "!")
                    .paragrafo(evento.nomeDaAluna() + " quer conversar com você. A solicitação é para:")
                    .detalhe("Data e hora", quando(evento.inicio()));

            if (evento.assunto() != null && !evento.assunto().isBlank()) {
                modelo.detalhe("Assunto", evento.assunto().trim());
            }

            modelo.paragrafo("Abra o aplicativo do TechMentor para confirmar. Se não puder nesse horário, "
                            + "você pode sugerir outro ou cancelar.")
                    .nota("Se a solicitação não for confirmada até o horário marcado, ela é cancelada automaticamente.");

            emailService.enviar(
                    evento.emailDaMentora(),
                    "Nova solicitação de mentoria (" + evento.inicio().format(CURTO) + ") - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o e-mail de nova solicitação de mentoria: {}", e.getMessage());
        }
    }

    private static String quando(LocalDateTime data) {
        String texto = data.format(QUANDO);
        return texto.substring(0, 1).toUpperCase(PT_BR) + texto.substring(1);
    }
}