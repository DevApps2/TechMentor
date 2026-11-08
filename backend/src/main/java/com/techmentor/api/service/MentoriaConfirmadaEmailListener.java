package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaConfirmada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** E-mail para a aluna quando a mentora confirma. Mesmas regras dos outros: depois de gravar, em outra thread, falha só vira log. */
@Component
public class MentoriaConfirmadaEmailListener {

    private static final Logger log = LoggerFactory.getLogger(MentoriaConfirmadaEmailListener.class);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    // Ex.: "segunda-feira, 02/11 às 14:00"
    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", PT_BR);

    // O lembrete sai junto com a abertura da sala (mesmo número do LembreteDeMentoriaService)
    private static final int MINUTOS_DO_LEMBRETE = LembreteDeMentoriaService.MINUTOS_PARA_ABRIR_SALA;

    private final EmailService emailService;

    public MentoriaConfirmadaEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(MentoriaConfirmada evento) {
        try {
            // Se faltam MINUTOS_DO_LEMBRETE ou menos, o lembrete (com o link) sai em até 1 minuto: mandar os dois seria e-mail repetido
            if (!evento.inicio().isAfter(LocalDateTime.now().plusMinutes(MINUTOS_DO_LEMBRETE))) {
                return;
            }

            String quando = evento.inicio().format(QUANDO);
            quando = quando.substring(0, 1).toUpperCase(PT_BR) + quando.substring(1);
            long minutos = Duration.between(evento.inicio(), evento.fim()).toMinutes();

            ModeloDeEmail modelo = ModeloDeEmail.com("Mentoria confirmada!")
                    .saudacao("Olá, " + evento.nomeDaAluna() + "!")
                    .paragrafo(evento.nomeDaMentora() + " confirmou a sua mentoria. Anote os detalhes:")
                    .detalhe("Quando", quando)
                    .detalhe("Mentora", evento.nomeDaMentora())
                    .detalhe("Duração", minutos + " minutos");

            if (evento.assunto() != null && !evento.assunto().isBlank()) {
                modelo.detalhe("Assunto", evento.assunto());
            }

            modelo.paragrafo("A conversa acontece por videochamada no Google Meet. A sala abre no aplicativo " + MINUTOS_DO_LEMBRETE + " minutos antes "
                    + "do início, e o link também chega por e-mail nesse momento.");

            modelo.nota("Precisa remarcar ou cancelar? Abra o aplicativo do TechMentor e use as opções da sua mentoria.");

            emailService.enviar(
                    evento.emailDaAluna(),
                    "Mentoria confirmada com " + evento.nomeDaMentora() + " - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o e-mail de confirmação da mentoria: {}", e.getMessage());
        }
    }
}