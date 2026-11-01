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

    private final ApplicationEventPublisher eventos;

    public AgendamentoService(HorarioDisponivelRepository horarios, MentoriaRepository mentorias,
                              BloqueioUsuarioRepository bloqueios, ConquistaService conquistas,
                              DesafioService desafios, ApplicationEventPublisher eventos) {
        this.horarios = horarios;
        this.mentorias = mentorias;
        this.bloqueios = bloqueios;
        this.conquistas = conquistas;
        this.desafios = desafios;
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
        mentoria.setStatus(Mentoria.Status.CANCELADA);
        mentoria.getHorario().setReservado(false);

        avisarCancelamento(actor, mentoria);

        return toResponse(mentoria);
    }
    /** Avisa a outra pessoa. Se quem cancelou foi o admin, avisa as duas. */
    private void avisarCancelamento(Usuario actor, Mentoria mentoria) {
        Usuario aluna = mentoria.getAluna();
        Usuario mentora = mentoria.getHorario().getMentora().getUsuario();
        LocalDateTime inicio = mentoria.getHorario().getInicio();

        if (actor.getId().equals(aluna.getId())) {
            eventos.publishEvent(new EventosDeNotificacao.MentoriaCancelada(mentora, actor.getNome(), inicio));
        } else if (actor.getId().equals(mentora.getId())) {
            eventos.publishEvent(new EventosDeNotificacao.MentoriaCancelada(aluna, actor.getNome(), inicio));
        } else {
            eventos.publishEvent(new EventosDeNotificacao.MentoriaCancelada(aluna, "A equipe do TechMentor", inicio));
            eventos.publishEvent(new EventosDeNotificacao.MentoriaCancelada(mentora, "A equipe do TechMentor", inicio));
        }
    }

    /** Só a mentora da mentoria (ou o admin). Passa de AGENDADA para CONFIRMADA e avisa a aluna. */
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

        eventos.publishEvent(new MentoriaConfirmada(
                mentoria.getAluna(), mentora.getNome(), mentoria.getHorario().getInicio()));

        return toResponse(mentoria);
    }

    /**
     * Troca a mentoria por outro horário livre da MESMA mentora.
     * Se quem pede é a aluna, volta para AGENDADA (a mentora confirma o novo horário);
     * se é a mentora (ou o admin), fica CONFIRMADA, porque foi ela quem escolheu.
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

        // Avisa a outra pessoa
        if (ehAluna) {
            eventos.publishEvent(new EventosDeNotificacao.MentoriaReagendada(mentora, actor.getNome(), novo.getInicio(), true));
        } else {
            String quem = ehMentora ? actor.getNome() : "A equipe do TechMentor";
            eventos.publishEvent(new EventosDeNotificacao.MentoriaReagendada(aluna, quem, novo.getInicio(), false));
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
        return toResponse(mentoria);
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
                horario.getInicio(), horario.getFim(), mentoria.getStatus(), mentoria.getAssunto(), mentoria.getCriadaEm());
    }
}
