package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeMentoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Manda o e-mail de lembrete da mentoria. Mesmas regras do aviso de denúncia:
 * só depois de gravar de verdade (AFTER_COMMIT), em outra thread (@Async), e uma falha só vira aviso no log.
 */
@Component
public class LembreteDeMentoriaEmailListener {

    private static final Logger log = LoggerFactory.getLogger(LembreteDeMentoriaEmailListener.class);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    // Ex.: "segunda-feira, 02/11 às 14:00"
    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final EmailService emailService;

    public LembreteDeMentoriaEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(LembreteDeMentoria lembrete) {
        try {
            String quando = lembrete.inicio().format(QUANDO);
            quando = quando.substring(0, 1).toUpperCase(PT_BR) + quando.substring(1);
            long minutos = Duration.between(lembrete.inicio(), lembrete.fim()).toMinutes();

            ModeloDeEmail modelo = ModeloDeEmail.com("Sua mentoria começa em breve")
                    .saudacao("Olá, " + lembrete.nomeDoDestinatario() + "!")
                    .paragrafo("Este é um lembrete da sua mentoria que acontece em instantes.")
                    .detalhe("Quando", quando)
                    .detalhe(lembrete.destinatarioEhMentora() ? "Aluna" : "Mentora", lembrete.outraPessoa())
                    .detalhe("Duração", minutos + " minutos");

            if (lembrete.assunto() != null && !lembrete.assunto().isBlank()) {
                modelo.detalhe("Assunto", lembrete.assunto());
            }

            modelo.paragrafo("A sala já está aberta. Entre pelo botão abaixo ou pelo aplicativo do TechMentor.");

            if (lembrete.linkSala() != null && !lembrete.linkSala().isBlank()) {
                modelo.botao("Entrar na sala", lembrete.linkSala());
            } else {
                modelo.paragrafo("O link da sala aparecerá no aplicativo assim que estiver pronto.");
            }

            modelo.nota("Precisa remarcar ou cancelar? Abra o aplicativo do TechMentor e use as opções da sua mentoria.");

            emailService.enviar(
                    lembrete.emailDoDestinatario(),
                    "Lembrete: sua mentoria começa às " + lembrete.inicio().format(HORA) + " - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o lembrete de mentoria por e-mail: {}", e.getMessage());
        }
    }
}