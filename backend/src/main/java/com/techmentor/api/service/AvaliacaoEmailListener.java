package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.AvaliacaoRecebida;
import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeAvaliacao;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaConcluida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * E-mails do ciclo de avaliação: convite ao terminar, lembrete de quem esqueceu e aviso de avaliação recebida.
 * Mesmas regras dos outros: depois de gravar, em outra thread, falha só vira log.
 */
@Component
public class AvaliacaoEmailListener {

    private static final Logger log = LoggerFactory.getLogger(AvaliacaoEmailListener.class);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    // Ex.: "segunda-feira, 02/11 às 14:00"
    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", PT_BR);
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;

    public AvaliacaoEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoConcluir(MentoriaConcluida evento) {
        try {
            String abertura = evento.destinatarioEhMentora()
                    ? "Sua mentoria com " + evento.outraPessoa() + " terminou. Deixe uma avaliação sobre a conversa: "
                    + "ela é privada, só a aluna e a equipe do TechMentor veem."
                    : "Sua mentoria com " + evento.outraPessoa() + " terminou. Conte como foi: "
                    + "sua avaliação ajuda outras alunas a escolherem a mentora certa.";

            ModeloDeEmail modelo = ModeloDeEmail.com("Como foi sua mentoria?")
                    .saudacao("Olá, " + evento.nomeDoDestinatario() + "!")
                    .paragrafo(abertura)
                    .detalhe("Mentoria", quando(evento.inicio()))
                    .detalhe(evento.destinatarioEhMentora() ? "Aluna" : "Mentora", evento.outraPessoa())
                    .detalhe("Avalie até", evento.prazoFinal().format(DIA))
                    .paragrafo("Abra o aplicativo do TechMentor: na tela inicial você encontra o aviso para avaliar. "
                            + "Leva menos de um minuto, de 1 a 5 estrelas e um comentário, se quiser.");

            emailService.enviar(
                    evento.emailDoDestinatario(),
                    "Como foi sua mentoria com " + evento.outraPessoa() + "? - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o convite de avaliação por e-mail: {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoLembrar(LembreteDeAvaliacao evento) {
        try {
            ModeloDeEmail modelo = ModeloDeEmail.com("Sua avaliação ainda está pendente")
                    .saudacao("Olá, " + evento.nomeDoDestinatario() + "!")
                    .paragrafo("Você ainda não avaliou sua mentoria com " + evento.outraPessoa()
                            + ". O prazo termina em " + evento.prazoFinal().format(DIA) + ".")
                    .detalhe("Mentoria", quando(evento.inicio()))
                    .detalhe(evento.destinatarioEhMentora() ? "Aluna" : "Mentora", evento.outraPessoa())
                    .detalhe("Avalie até", evento.prazoFinal().format(DIA))
                    .paragrafo("Abra o aplicativo do TechMentor e avalie na tela inicial. Leva menos de um minuto.")
                    .nota("Este é o único lembrete que enviamos sobre esta avaliação.");

            emailService.enviar(
                    evento.emailDoDestinatario(),
                    "Falta avaliar sua mentoria com " + evento.outraPessoa() + " - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o lembrete de avaliação por e-mail: {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoReceber(AvaliacaoRecebida evento) {
        try {
            ModeloDeEmail modelo = ModeloDeEmail.com("Você recebeu uma avaliação")
                    .saudacao("Olá, " + evento.nomeDoDestinatario() + "!")
                    .paragrafo(evento.quemAvaliou() + " avaliou a mentoria com você:")
                    .detalhe("Nota", estrelas(evento.nota()) + " (" + evento.nota() + " de 5)");

            if (evento.comentario() != null && !evento.comentario().isBlank()) {
                modelo.detalhe("Comentário", evento.comentario());
            }

            modelo.nota(evento.destinatarioEhMentora()
                    ? "Esta avaliação aparece no seu perfil de mentora, com o nome da aluna abreviado."
                    : "Este feedback é privado: só você e a equipe do TechMentor veem.");

            emailService.enviar(
                    evento.emailDoDestinatario(),
                    "Você recebeu uma avaliação - TechMentor",
                    modelo
            );
        } catch (Exception e) {
            log.warn("Não foi possível enviar o aviso de avaliação recebida por e-mail: {}", e.getMessage());
        }
    }

    private static String quando(LocalDateTime data) {
        String texto = data.format(QUANDO);
        return texto.substring(0, 1).toUpperCase(PT_BR) + texto.substring(1);
    }

    /** 4 vira "★★★★☆". */
    private static String estrelas(int nota) {
        return "★".repeat(nota) + "☆".repeat(5 - nota);
    }
}