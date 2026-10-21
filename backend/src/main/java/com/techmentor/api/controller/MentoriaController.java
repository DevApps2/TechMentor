package com.techmentor.api.controller;

import com.techmentor.api.dto.MentoriaDtos.*;
import com.techmentor.api.service.AgendamentoService;
import com.techmentor.api.service.MentoraService;
import com.techmentor.api.service.UsuarioAtualService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MentoriaController {
    private final MentoraService mentoras;
    private final AgendamentoService agendamentos;
    private final UsuarioAtualService usuarios;

    public MentoriaController(MentoraService mentoras, AgendamentoService agendamentos, UsuarioAtualService usuarios) {
        this.mentoras = mentoras;
        this.agendamentos = agendamentos;
        this.usuarios = usuarios;
    }

    @GetMapping("/areas-atuacao")
    public List<AreaResponse> listarAreas() { return mentoras.listarAreas(); }

    @PostMapping("/areas-atuacao")
    @ResponseStatus(HttpStatus.CREATED)
    public AreaResponse criarArea(@RequestHeader("X-Usuario-Id") Long actorId, @RequestBody AreaRequest request) {
        return mentoras.criarArea(usuarios.buscar(actorId), request == null ? null : request.nome());
    }

    @GetMapping("/mentoras")
    public List<MentoraResponse> listarMentoras() { return mentoras.listarMentoras(); }

    @GetMapping("/mentoras/{id}")
    public MentoraResponse obterMentora(@PathVariable Long id) { return mentoras.obterMentora(id); }

    @PostMapping("/mentoras")
    @ResponseStatus(HttpStatus.CREATED)
    public MentoraResponse criarMentora(@RequestHeader("X-Usuario-Id") Long actorId,
                                        @RequestBody MentoraRequest request) {
        return mentoras.criarMentora(usuarios.buscar(actorId), request);
    }

    @PutMapping("/mentoras/{id}")
    public MentoraResponse atualizarMentora(@RequestHeader("X-Usuario-Id") Long actorId,
                                            @PathVariable Long id,
                                            @RequestBody MentoraRequest request) {
        return mentoras.atualizarMentora(usuarios.buscar(actorId), id, request);
    }

    @DeleteMapping("/mentoras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirMentora(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id) {
        mentoras.excluirMentora(usuarios.buscar(actorId), id);
    }

    @GetMapping("/mentoras/{id}/disponibilidades")
    public List<HorarioResponse> listarHorarios(@PathVariable Long id) { return mentoras.listarHorarios(id); }

    @PostMapping("/mentoras/{id}/disponibilidades")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioResponse criarHorario(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id,
                                        @RequestBody HorarioRequest request) {
        return mentoras.criarHorario(usuarios.buscar(actorId), id, request);
    }

    @DeleteMapping("/disponibilidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirHorario(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id) {
        mentoras.excluirHorario(usuarios.buscar(actorId), id);
    }

    @GetMapping("/agendamentos")
    public List<AgendamentoResponse> listarAgendamentos(@RequestHeader("X-Usuario-Id") Long actorId) {
        return agendamentos.listar(usuarios.buscar(actorId));
    }

    @GetMapping("/agendamentos/{id}")
    public AgendamentoResponse obterAgendamento(@RequestHeader("X-Usuario-Id") Long actorId,
                                                @PathVariable Long id) {
        return agendamentos.obter(usuarios.buscar(actorId), id);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criarAgendamento(@RequestHeader("X-Usuario-Id") Long actorId,
                                               @RequestBody AgendamentoRequest request) {
        return agendamentos.criar(usuarios.buscar(actorId), request);
    }

    @PatchMapping("/agendamentos/{id}/cancelamento")
    public AgendamentoResponse cancelarAgendamento(@RequestHeader("X-Usuario-Id") Long actorId,
                                                   @PathVariable Long id) {
        return agendamentos.cancelar(usuarios.buscar(actorId), id);
    }
}
