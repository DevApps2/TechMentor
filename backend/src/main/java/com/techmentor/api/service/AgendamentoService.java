package com.techmentor.api.service;

import com.techmentor.api.dto.MentoriaDtos.AgendamentoRequest;
import com.techmentor.api.dto.MentoriaDtos.AgendamentoResponse;
import com.techmentor.api.evento.EventosDeNotificacao;
import com.techmentor.api.repository.HorarioDisponivelRepository;
import com.techmentor.api.repository.MentoriaRepository;
import com.techmentor.api.model.HorarioDisponivel;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.BloqueioUsuarioRepository;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.sala.GeradorDeSala;
import com.techmentor.api.sala.GeradorDeSala.DadosDaSala;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaConfirmada;

@Service
@Transactional
public class AgendamentoService {
    private final HorarioDisponivelRepository horarios;
    private final MentoriaRepository mentorias;
    private final BloqueioUsuarioRepository bloqueios;
    private final ConquistaService conquistas;
    private final DesafioService desafios;
    private final GeradorDeSala salas;

    private final ApplicationEventPublisher eventos;

    public AgendamentoService(HorarioDisponivelRepository horarios, MentoriaRepository mentorias,
                              BloqueioUsuarioRepository bloqueios, ConquistaService conquistas,
                              DesafioService desafios, GeradorDeSala salas, ApplicationEventPublisher eventos) {
        this.horarios = horarios;
        this.mentorias = mentorias;
        this.bloqueios = bloqueios;
        this.conquistas = conquistas;
        this.desafios = desafios;
        this.salas = salas;
        this.eventos = eventos;
    }
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(Usuario actor) {
        List<Mentoria> items;
        if (actor.getTipo() == Usuario.Tipo.ADMIN) {
            items = mentorias.findAll();
        } else {
            items = new ArrayList<>(mentorias.findByAlunaIdOrderByHorarioInicioDesc(actor.getId()));
            Map<Long, Mentoria> vistos = new HashMap<>();
            items.forEach(item -> vistos.put(item.getId(), item));
            mentorias.findByHorarioMentoraUsuarioIdOrderByHorarioInicioDesc(actor.getId())
                    .forEach(item -> vistos.put(item.getId(), item));
            items = new ArrayList<>(vistos.values());
        }
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse obter(Usuario actor, Long id) {
        Mentoria mentoria = buscar(id);
        boolean participante = mentoria.getAluna().getId().equals(actor.getId())
                || mentoria.getHorario().getMentora().getUsuario().getId().equals(actor.getId());
        if (actor.getTipo() != Usuario.Tipo.ADMIN && !participante) throw ApiException.forbidden();
        return toResponse(mentoria);
    }

    public AgendamentoResponse criar(Usuario actor, AgendamentoRequest request) {
        if (actor.getTipo() != Usuario.Tipo.ALUNA) throw ApiException.forbidden();
        if (request == null || request.horarioId() == null)
            throw ApiException.badRequest("Horário é obrigatório.");
        HorarioDisponivel horario = horarios.findByIdForUpdate(request.horarioId())
                .orElseThrow(() -> ApiException.notFound("Horário não encontrado."));
        if (horario.isReservado() || !horario.getInicio().isAfter(LocalDateTime.now()))
            throw ApiException.conflict("Horário indisponível.");
        Long mentoraUsuarioId = horario.getMentora().getUsuario().getId();
        if (actor.getId().equals(mentoraUsuarioId) || estaBloqueado(actor.getId(), mentoraUsuarioId))
            throw ApiException.conflict("Não é possível agendar com este usuário.");

        horario.setReservado(true);
        Mentoria mentoria = new Mentoria();
        mentoria.setAluna(actor);
        mentoria.setHorario(horario);
        mentoria.setAssunto(request.assunto());
        Mentoria salva = mentorias.save(mentoria);

        // A primeira reserva desbloqueia a conquista; desafios de mentoria também são verificados aqui.
        conquistas.desbloquear(actor, ConquistaService.PRIMEIRA_MENTORIA);
        desafios.verificarDesafios(actor);

        Usuario mentoraDaMentoria = horario.getMentora().getUsuario();
        eventos.publishEvent(new EventosDeNotificacao.MentoriaAgendada(
                mentoraDaMentoria, actor.getNome(), horario.getInicio(),
                mentoraDaMentoria.getNome(), mentoraDaMentoria.getEmail(), request.assunto()));

        return toResponse(salva);
    }

    /** Avisa a outra pessoa. Se quem cancelou foi o admin, avisa as duas. */
    private void avisarCancelamento(Usuario actor, Mentoria mentoria) {
        Usuario aluna = mentoria.getAluna();
        Usuario mentora = mentoria.getHorario().getMentora().getUsuario();
        LocalDateTime inicio = mentoria.getHorario().getInicio();

        if (actor.getId().equals(aluna.getId())) {
            eventos.publishEvent(cancelada(mentora, actor.getNome(), inicio));
        } else if (actor.getId().equals(mentora.getId())) {
            eventos.publishEvent(cancelada(aluna, actor.getNome(), inicio));
        } else {
            eventos.publishEvent(cancelada(aluna, "A equipe do TechMentor", inicio));
            eventos.publishEvent(cancelada(mentora, "A equipe do TechMentor", inicio));
        }
    }

    /** Só a mentora da mentoria (ou o admin). Passa de AGENDADA para CONFIRMADA, cria a sala e avisa a aluna. */
    public AgendamentoResponse confirmar(Usuario actor, Long id) {
        Mentoria mentoria = buscar(id);
        Usuario mentora = mentoria.getHorario().getMentora().getUsuario();

        if (!actor.getId().equals(mentora.getId()) && actor.getTipo() != Usuario.Tipo.ADMIN)
            throw ApiException.forbidden();
        if (mentoria.getStatus() != Mentoria.Status.AGENDADA)
            throw ApiException.conflict("Só mentorias aguardando confirmação podem ser confirmadas.");
        if (!mentoria.getHorario().getInicio().isAfter(LocalDateTime.now()))
            throw ApiException.conflict("Esta mentoria já passou do horário.");

        mentoria.setStatus(Mentoria.Status.CONFIRMADA);
        garantirSala(mentoria);

        eventos.publishEvent(new MentoriaConfirmada(
                mentoria.getAluna(), mentora.getNome(), mentoria.getHorario().getInicio(),
                mentoria.getHorario().getFim(), mentoria.getLinkSala(),
                mentoria.getAluna().getNome(), mentoria.getAluna().getEmail(), mentoria.getAssunto()));

        return toResponse(mentoria);
    }

    /**
     * Troca a mentoria por outro horário livre da MESMA mentora.
     * Se quem pede é a aluna, volta para AGENDADA (a mentora confirma o novo horário);
     * se é a mentora (ou o admin), fica CONFIRMADA, porque foi ela quem escolheu.
     * A sala do horário antigo é apagada; só existe sala com a mentoria CONFIRMADA.
     */
    public AgendamentoResponse reagendar(Usuario actor, Long id, Long novoHorarioId) {
        Mentoria mentoria = buscar(id);
        Usuario aluna = mentoria.getAluna();
        Usuario mentora = mentoria.getHorario().getMentora().getUsuario();

        boolean ehAluna = actor.getId().equals(aluna.getId());
        boolean ehMentora = actor.getId().equals(mentora.getId());
        if (!ehAluna && !ehMentora && actor.getTipo() != Usuario.Tipo.ADMIN) throw ApiException.forbidden();

        if (novoHorarioId == null) throw ApiException.badRequest("Horário é obrigatório.");
        if (mentoria.getStatus() != Mentoria.Status.AGENDADA && mentoria.getStatus() != Mentoria.Status.CONFIRMADA)
            throw ApiException.conflict("Só mentorias agendadas ou confirmadas podem ser reagendadas.");

        HorarioDisponivel antigo = mentoria.getHorario();
        if (!antigo.getInicio().isAfter(LocalDateTime.now()))
            throw ApiException.conflict("Esta mentoria já começou e não pode ser reagendada.");
        if (antigo.getId().equals(novoHorarioId))
            throw ApiException.badRequest("Escolha um horário diferente do atual.");

        // Mesmo bloqueio do agendar: duas pessoas não reservam o mesmo horário ao mesmo tempo
        HorarioDisponivel novo = horarios.findByIdForUpdate(novoHorarioId)
                .orElseThrow(() -> ApiException.notFound("Horário não encontrado."));
        if (!novo.getMentora().getId().equals(antigo.getMentora().getId()))
            throw ApiException.badRequest("O novo horário precisa ser da mesma mentora.");
        if (novo.isReservado() || !novo.getInicio().isAfter(LocalDateTime.now()))
            throw ApiException.conflict("Horário indisponível.");

        antigo.setReservado(false);
        novo.setReservado(true);
        mentoria.setHorario(novo);
        mentoria.setStatus(ehAluna ? Mentoria.Status.AGENDADA : Mentoria.Status.CONFIRMADA);

        // A sala do horário antigo não vale mais; se já está confirmada, cria a do novo
        apagarSala(mentoria);
        if (mentoria.getStatus() == Mentoria.Status.CONFIRMADA) garantirSala(mentoria);
        mentoria.setLembreteEnviadoEm(null);

        // Avisa a outra pessoa
        if (ehAluna) {
            eventos.publishEvent(reagendada(mentora, actor.getNome(), novo.getInicio(), true));
        } else {
            String quem = ehMentora ? actor.getNome() : "A equipe do TechMentor";
            eventos.publishEvent(reagendada(aluna, quem, novo.getInicio(), false));
        }

        return toResponse(mentoria);
    }

    public AgendamentoResponse cancelar(Usuario actor, Long id) {
        Mentoria mentoria = buscar(id);
        boolean participante = mentoria.getAluna().getId().equals(actor.getId())
                || mentoria.getHorario().getMentora().getUsuario().getId().equals(actor.getId());
        if (!participante && actor.getTipo() != Usuario.Tipo.ADMIN) throw ApiException.forbidden();
        if (mentoria.getStatus() == Mentoria.Status.CANCELADA)
            throw ApiException.conflict("Agendamento já cancelado.");
        if (mentoria.getStatus() == Mentoria.Status.CONCLUIDA)
            throw ApiException.conflict("Agendamento concluído não pode ser cancelado.");
        mentoria.setStatus(Mentoria.Status.CANCELADA);
        mentoria.getHorario().setReservado(false);
        apagarSala(mentoria);

        avisarCancelamento(actor, mentoria);

        return toResponse(mentoria);
    }

    /**
     * Chamado pelo relógio (não é rota): a mentoria ficou AGENDADA e o horário começou sem a mentora confirmar.
     * Cancela e avisa as duas pessoas. O horário continua reservado de propósito: ele já passou,
     * e liberar faria um horário antigo aparecer como disponível no perfil da mentora.
     */
    public void expirar(Mentoria mentoria) {
        if (mentoria.getStatus() != Mentoria.Status.AGENDADA) return;

        mentoria.setStatus(Mentoria.Status.CANCELADA);

        Usuario aluna = mentoria.getAluna();
        Usuario mentora = mentoria.getHorario().getMentora().getUsuario();
        LocalDateTime inicio = mentoria.getHorario().getInicio();

        eventos.publishEvent(new EventosDeNotificacao.MentoriaExpirada(aluna, mentora.getNome(), inicio, false));
        eventos.publishEvent(new EventosDeNotificacao.MentoriaExpirada(mentora, aluna.getNome(), inicio, true));
    }

    /**
     * Cria a sala do Google Meet se a mentoria ainda não tem uma. Se o Google falhar ou não estiver configurado,
     * a mentoria segue sem link (a confirmação não quebra) e uma nova tentativa pode ser feita depois.
     */
    public void garantirSala(Mentoria mentoria) {
        if (mentoria.getLinkSala() != null) return;

        HorarioDisponivel horario = mentoria.getHorario();
        Usuario aluna = mentoria.getAluna();
        Usuario mentora = horario.getMentora().getUsuario();

        String descricao = "Mentoria marcada pelo TechMentor entre " + aluna.getNome() + " e " + mentora.getNome() + "."
                + (mentoria.getAssunto() == null || mentoria.getAssunto().isBlank()
                ? "" : "\nAssunto: " + mentoria.getAssunto());

        salas.criar(new DadosDaSala(
                "Mentoria TechMentor: " + aluna.getNome() + " e " + mentora.getNome(),
                descricao,
                horario.getInicio(),
                horario.getFim(),
                List.of(aluna.getEmail(), mentora.getEmail())
        )).ifPresent(sala -> {
            mentoria.setLinkSala(sala.link());
            mentoria.setGoogleEventoId(sala.idDoEvento());
        });
    }

    private void apagarSala(Mentoria mentoria) {
        if (mentoria.getGoogleEventoId() != null) salas.apagar(mentoria.getGoogleEventoId());
        mentoria.setLinkSala(null);
        mentoria.setGoogleEventoId(null);
    }

    /** Evento de cancelamento já com nome e e-mail em texto simples (o e-mail sai em outra thread, sem acesso ao banco). */
    private EventosDeNotificacao.MentoriaCancelada cancelada(Usuario destinatario, String quemCancelou, LocalDateTime inicio) {
        return new EventosDeNotificacao.MentoriaCancelada(
                destinatario, quemCancelou, inicio, destinatario.getNome(), destinatario.getEmail());
    }

    private EventosDeNotificacao.MentoriaReagendada reagendada(Usuario destinatario, String quemReagendou,
                                                               LocalDateTime novoInicio, boolean precisaConfirmar) {
        return new EventosDeNotificacao.MentoriaReagendada(
                destinatario, quemReagendou, novoInicio, precisaConfirmar, destinatario.getNome(), destinatario.getEmail());
    }

    private boolean estaBloqueado(Long primeiroUsuarioId, Long segundoUsuarioId) {
        return bloqueios.existsByBloqueadorIdAndBloqueadoIdOrBloqueadorIdAndBloqueadoId(
                primeiroUsuarioId, segundoUsuarioId, segundoUsuarioId, primeiroUsuarioId);
    }

    private Mentoria buscar(Long id) {
        return mentorias.findById(id).orElseThrow(() -> ApiException.notFound("Agendamento não encontrado."));
    }

    private AgendamentoResponse toResponse(Mentoria mentoria) {
        HorarioDisponivel horario = mentoria.getHorario();
        return new AgendamentoResponse(mentoria.getId(), mentoria.getAluna().getId(), mentoria.getAluna().getNome(),
                horario.getMentora().getId(), horario.getMentora().getUsuario().getNome(), horario.getId(),
                horario.getInicio(), horario.getFim(), mentoria.getStatus(), mentoria.getAssunto(), mentoria.getCriadaEm(),
                mentoria.getLinkSala(), LembreteDeMentoriaService.MINUTOS_PARA_ABRIR_SALA);
    }
}
