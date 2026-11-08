package com.techmentor.api.service;

import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeAvaliacao;
import com.techmentor.api.evento.EventosDeNotificacao.LembreteDeMentoria;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaConcluida;
import com.techmentor.api.model.HorarioDisponivel;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.AvaliacaoRepository;
import com.techmentor.api.repository.MentoriaRepository;
import com.techmentor.api.sala.GeradorDeSala;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Trabalho que roda de minuto em minuto:
 * 1) marca como CONCLUIDA as mentorias confirmadas que já terminaram e convida as duas pessoas a avaliar;
 * 2) lembra, uma vez só, quem ainda não avaliou depois de alguns dias;
 * 3) tenta criar de novo a sala das mentorias confirmadas que ficaram sem link (Google fora do ar na hora de confirmar);
 * 4) avisa as duas pessoas das mentorias confirmadas que começam em breve, uma vez só.
 */
@Service
public class LembreteDeMentoriaService {

    /** A sala abre no app este tanto antes do início. Tem que ser igual ao MINUTOS_ANTES do app (BotaoEntrarNaSala). */
    private static final int MINUTOS_PARA_ABRIR_SALA = 30;

    /** O lembrete sai junto com a abertura da sala, para o link do e-mail só chegar quando já pode ser usado. */
    private static final int MINUTOS_DE_ANTECEDENCIA = MINUTOS_PARA_ABRIR_SALA;

    /** Só vale a pena tentar criar a sala de mentorias das próximas horas. */
    private static final int HORAS_PARA_TENTAR_SALA = 24;

    /** Prazo para avaliar. Mentoria que terminou há mais que isso é concluída em silêncio, sem convite. */
    public static final int DIAS_PARA_AVALIAR = 14;

    /** Quantos dias depois do fim a pessoa que não avaliou recebe o lembrete. */
    private static final int DIAS_ATE_LEMBRAR_DA_AVALIACAO = 3;

    private final MentoriaRepository mentorias;
    private final AvaliacaoRepository avaliacoes;
    private final AgendamentoService agendamentos;
    private final GeradorDeSala salas;
    private final ApplicationEventPublisher eventos;

    public LembreteDeMentoriaService(MentoriaRepository mentorias, AvaliacaoRepository avaliacoes,
                                     AgendamentoService agendamentos, GeradorDeSala salas,
                                     ApplicationEventPublisher eventos) {
        this.mentorias = mentorias;
        this.avaliacoes = avaliacoes;
        this.agendamentos = agendamentos;
        this.salas = salas;
        this.eventos = eventos;
    }

    @Transactional
    public void processar() {
        LocalDateTime agora = LocalDateTime.now();

        // 0) Mentorias que ninguém confirmou e cujo horário já começou: expiram (e avisam as duas pessoas)
        expirarNaoConfirmadas(agora);

        // 1) Mentorias que terminaram viram CONCLUIDA (e convidam a avaliar)
        concluirTerminadas(agora);

        // 2) Quem esqueceu de avaliar
        lembrarDeAvaliar(agora);

        // 3) Sala que faltou. Só tenta se o Google está configurado, para não encher o log à toa.
        if (salas.configurado()) {
            mentorias.findByStatusAndLinkSalaIsNullAndHorarioInicioBetween(
                            Mentoria.Status.CONFIRMADA, agora, agora.plusHours(HORAS_PARA_TENTAR_SALA))
                    .forEach(agendamentos::garantirSala);
        }

        // 4) Lembretes
        mentorias.findByStatusAndLembreteEnviadoEmIsNullAndHorarioInicioBetween(
                        Mentoria.Status.CONFIRMADA, agora, agora.plusMinutes(MINUTOS_DE_ANTECEDENCIA))
                .forEach(mentoria -> lembrar(mentoria, agora));
    }

    /**
     * As AGENDADAS cujo horário já começou sem a mentora confirmar viram CANCELADA.
     * A regra de "o que fazer" fica no AgendamentoService.expirar; aqui só se escolhe quais.
     */
    private void expirarNaoConfirmadas(LocalDateTime agora) {
        mentorias.findByStatusAndHorarioInicioBefore(Mentoria.Status.AGENDADA, agora)
                .forEach(agendamentos::expirar);
    }

    /**
     * Só as CONFIRMADAS viram CONCLUIDA: uma mentoria que ninguém confirmou não aconteceu.
     * O convite para avaliar só sai se ela terminou dentro do prazo de avaliação.
     */
    private void concluirTerminadas(LocalDateTime agora) {
        LocalDateTime limiteDoConvite = agora.minusDays(DIAS_PARA_AVALIAR);

        mentorias.findByStatusAndHorarioFimBefore(Mentoria.Status.CONFIRMADA, agora).forEach(mentoria -> {

            mentoria.setStatus(Mentoria.Status.CONCLUIDA);

            HorarioDisponivel horario = mentoria.getHorario();
            if (horario.getFim().isBefore(limiteDoConvite)) {
                return;
            }

            Usuario aluna = mentoria.getAluna();
            Usuario mentora = horario.getMentora().getUsuario();
            LocalDateTime prazoFinal = horario.getFim().plusDays(DIAS_PARA_AVALIAR);

            eventos.publishEvent(new MentoriaConcluida(
                    aluna, aluna.getNome(), aluna.getEmail(), mentora.getNome(), false, horario.getInicio(), prazoFinal));

            eventos.publishEvent(new MentoriaConcluida(
                    mentora, mentora.getNome(), mentora.getEmail(), aluna.getNome(), true, horario.getInicio(), prazoFinal));
        });
    }

    /**
     * Mentorias concluídas há uns dias, ainda dentro do prazo: quem não avaliou recebe um lembrete.
     * A marca na mentoria vale para as duas pessoas, então o lembrete sai uma única vez.
     */
    private void lembrarDeAvaliar(LocalDateTime agora) {
        LocalDateTime maisAntigas = agora.minusDays(DIAS_PARA_AVALIAR);
        LocalDateTime maisNovas = agora.minusDays(DIAS_ATE_LEMBRAR_DA_AVALIACAO);

        mentorias.findByStatusAndLembreteAvaliacaoEnviadoEmIsNullAndHorarioFimBetween(
                        Mentoria.Status.CONCLUIDA, maisAntigas, maisNovas)
                .forEach(mentoria -> {

                    HorarioDisponivel horario = mentoria.getHorario();
                    Usuario aluna = mentoria.getAluna();
                    Usuario mentora = horario.getMentora().getUsuario();
                    LocalDateTime prazoFinal = horario.getFim().plusDays(DIAS_PARA_AVALIAR);

                    if (!avaliacoes.existsByMentoriaIdAndAvaliadorId(mentoria.getId(), aluna.getId())) {
                        eventos.publishEvent(new LembreteDeAvaliacao(
                                aluna, aluna.getNome(), aluna.getEmail(), mentora.getNome(), false,
                                horario.getInicio(), prazoFinal));
                    }

                    if (!avaliacoes.existsByMentoriaIdAndAvaliadorId(mentoria.getId(), mentora.getId())) {
                        eventos.publishEvent(new LembreteDeAvaliacao(
                                mentora, mentora.getNome(), mentora.getEmail(), aluna.getNome(), true,
                                horario.getInicio(), prazoFinal));
                    }

                    mentoria.setLembreteAvaliacaoEnviadoEm(agora);
                });
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