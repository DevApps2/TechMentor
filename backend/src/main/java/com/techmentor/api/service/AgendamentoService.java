package com.techmentor.api.service;

import com.techmentor.api.dto.MentoriaDtos.AgendamentoRequest;
import com.techmentor.api.dto.MentoriaDtos.AgendamentoResponse;
import com.techmentor.api.repository.HorarioDisponivelRepository;
import com.techmentor.api.repository.MentoriaRepository;
import com.techmentor.api.model.HorarioDisponivel;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.BloqueioUsuarioRepository;
import com.techmentor.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AgendamentoService {
    private final HorarioDisponivelRepository horarios;
    private final MentoriaRepository mentorias;
    private final BloqueioUsuarioRepository bloqueios;

    public AgendamentoService(HorarioDisponivelRepository horarios, MentoriaRepository mentorias,
                              BloqueioUsuarioRepository bloqueios) {
        this.horarios = horarios;
        this.mentorias = mentorias;
        this.bloqueios = bloqueios;
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
        return toResponse(mentorias.save(mentoria));
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
