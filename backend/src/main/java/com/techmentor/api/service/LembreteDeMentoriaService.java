package com.techmentor.api.service;

import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeMentoria;
import com.techmentor.api.model.HorarioDisponivel;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.MentoriaRepository;
import com.techmentor.api.sala.GeradorDeSala;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Trabalho que roda de minuto em minuto:
 * 1) tenta criar de novo a sala das mentorias confirmadas que ficaram sem link (Google fora do ar na hora de confirmar);
 * 2) avisa as duas pessoas das mentorias confirmadas que começam na próxima hora, uma vez só.
 */
@Service
public class LembreteDeMentoriaService {

    /** A sala abre no app este tanto antes do início. Tem que ser igual ao MINUTOS_ANTES do app (BotaoEntrarNaSala). */
    private static final int MINUTOS_PARA_ABRIR_SALA = 30;

    /** O lembrete sai junto com a abertura da sala, para o link do e-mail só chegar quando já pode ser usado. */
    private static final int MINUTOS_DE_ANTECEDENCIA = MINUTOS_PARA_ABRIR_SALA;

    /** Só vale a pena tentar criar a sala de mentorias das próximas horas. */
    private static final int HORAS_PARA_TENTAR_SALA = 24;

    private final MentoriaRepository mentorias;
    private final AgendamentoService agendamentos;
    private final GeradorDeSala salas;
    private final ApplicationEventPublisher eventos;

    public LembreteDeMentoriaService(MentoriaRepository mentorias, AgendamentoService agendamentos,
                                     GeradorDeSala salas, ApplicationEventPublisher eventos) {
        this.mentorias = mentorias;
        this.agendamentos = agendamentos;
        this.salas = salas;
        this.eventos = eventos;
    }

    @Transactional
    public void processar() {
        LocalDateTime agora = LocalDateTime.now();

        // 1) Sala que faltou. Só tenta se o Google está configurado, para não encher o log à toa.
        if (salas.configurado()) {
            mentorias.findByStatusAndLinkSalaIsNullAndHorarioInicioBetween(
                            Mentoria.Status.CONFIRMADA, agora, agora.plusHours(HORAS_PARA_TENTAR_SALA))
                    .forEach(agendamentos::garantirSala);
        }

        // 2) Lembretes
        mentorias.findByStatusAndLembreteEnviadoEmIsNullAndHorarioInicioBetween(
                        Mentoria.Status.CONFIRMADA, agora, agora.plusMinutes(MINUTOS_DE_ANTECEDENCIA))
                .forEach(mentoria -> lembrar(mentoria, agora));
    }

    private void lembrar(Mentoria mentoria, LocalDateTime agora) {

        HorarioDisponivel horario = mentoria.getHorario();
        Usuario aluna = mentoria.getAluna();
        Usuario mentora = horario.getMentora().getUsuario();
        LocalDateTime salaAbreEm = horario.getInicio().minusMinutes(MINUTOS_PARA_ABRIR_SALA);

        eventos.publishEvent(new LembreteDeMentoria(
                aluna, aluna.getNome(), aluna.getEmail(), mentora.getNome(), false,
                horario.getInicio(), horario.getFim(), salaAbreEm, mentoria.getLinkSala(), mentoria.getAssunto()));

        eventos.publishEvent(new LembreteDeMentoria(
                mentora, mentora.getNome(), mentora.getEmail(), aluna.getNome(), true,
                horario.getInicio(), horario.getFim(), salaAbreEm, mentoria.getLinkSala(), mentoria.getAssunto()));

        mentoria.setLembreteEnviadoEm(agora);
    }
}