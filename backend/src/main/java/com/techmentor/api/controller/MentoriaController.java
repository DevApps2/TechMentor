package com.techmentor.api.controller;

import com.techmentor.api.dto.MentoriaDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.techmentor.api.service.AgendamentoService;
import com.techmentor.api.service.MentoraService;
import com.techmentor.api.service.UsuarioAtualService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Mentoria", description = "Áreas de atuação, mentoras, disponibilidades e agendamentos de mentoria")
public class MentoriaController {
    private final MentoraService mentoras;
    private final AgendamentoService agendamentos;
    private final UsuarioAtualService usuarios;

    public MentoriaController(MentoraService mentoras, AgendamentoService agendamentos, UsuarioAtualService usuarios) {
        this.mentoras = mentoras;
        this.agendamentos = agendamentos;
        this.usuarios = usuarios;
    }

    @Operation(summary = "Listar áreas de atuação")
    @GetMapping("/areas-atuacao")
    public List<AreaResponse> listarAreas() { return mentoras.listarAreas(); }

    @Operation(summary = "Criar área de atuação")
    @PostMapping("/areas-atuacao")
    @ResponseStatus(HttpStatus.CREATED)
    public AreaResponse criarArea(@RequestHeader("X-Usuario-Id") Long actorId,
                                  @Valid @RequestBody AreaRequest request) {
        return mentoras.criarArea(usuarios.buscar(actorId), request == null ? null : request.nome());
    }

    @Operation(summary = "Listar mentoras", description = "Busca opcional por nome, cargo ou área, e filtro por uma área (nome exato).")
    @GetMapping("/mentoras")
    public List<MentoraResponse> listarMentoras(
            @Parameter(description = "Texto para buscar no nome, cargo ou áreas (opcional)") @RequestParam(required = false) String busca,
            @Parameter(description = "Nome exato de uma área de atuação (opcional)") @RequestParam(required = false) String area) {
        return mentoras.listarMentoras(busca, area);
    }

    @Operation(summary = "Obter mentora por ID")
    @GetMapping("/mentoras/{id}")
    public MentoraResponse obterMentora(@Parameter(description = "ID da mentora") @PathVariable Long id) { return mentoras.obterMentora(id); }

    @Operation(summary = "Criar perfil de mentora")
    @PostMapping("/mentoras")
    @ResponseStatus(HttpStatus.CREATED)
    public MentoraResponse criarMentora(@RequestHeader("X-Usuario-Id") Long actorId,
                                        @Valid @RequestBody MentoraRequest request) {
        return mentoras.criarMentora(usuarios.buscar(actorId), request);
    }

    @Operation(summary = "Atualizar perfil de mentora")
    @PutMapping("/mentoras/{id}")
    public MentoraResponse atualizarMentora(@RequestHeader("X-Usuario-Id") Long actorId,
                                            @Parameter(description = "ID da mentora") @PathVariable Long id,
                                            @Valid @RequestBody MentoraRequest request) {
        return mentoras.atualizarMentora(usuarios.buscar(actorId), id, request);
    }

    @Operation(summary = "Excluir perfil de mentora")
    @DeleteMapping("/mentoras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirMentora(@RequestHeader("X-Usuario-Id") Long actorId,
                               @Parameter(description = "ID da mentora") @PathVariable Long id) {
        mentoras.excluirMentora(usuarios.buscar(actorId), id);
    }

    @Operation(summary = "Listar disponibilidades de uma mentora")
    @GetMapping("/mentoras/{id}/disponibilidades")
    public List<HorarioResponse> listarHorarios(@Parameter(description = "ID da mentora") @PathVariable Long id) {
        return mentoras.listarHorarios(id);
    }

    @Operation(summary = "Criar disponibilidade de uma mentora")
    @PostMapping("/mentoras/{id}/disponibilidades")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioResponse criarHorario(@RequestHeader("X-Usuario-Id") Long actorId,
                                        @Parameter(description = "ID da mentora") @PathVariable Long id,
                                        @Valid @RequestBody HorarioRequest request) {
        return mentoras.criarHorario(usuarios.buscar(actorId), id, request);
    }

    @Operation(summary = "Excluir disponibilidade")
    @DeleteMapping("/disponibilidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirHorario(@RequestHeader("X-Usuario-Id") Long actorId,
                               @Parameter(description = "ID da disponibilidade") @PathVariable Long id) {
        mentoras.excluirHorario(usuarios.buscar(actorId), id);
    }

    @Operation(summary = "Listar agendamentos do usuário")
    @GetMapping("/agendamentos")
    public List<AgendamentoResponse> listarAgendamentos(@RequestHeader("X-Usuario-Id") Long actorId) {
        return agendamentos.listar(usuarios.buscar(actorId));
    }

    @Operation(summary = "Obter agendamento por ID")
    @GetMapping("/agendamentos/{id}")
    public AgendamentoResponse obterAgendamento(@RequestHeader("X-Usuario-Id") Long actorId,
                                                @Parameter(description = "ID do agendamento") @PathVariable Long id) {
        return agendamentos.obter(usuarios.buscar(actorId), id);
    }

    @Operation(summary = "Criar agendamento de mentoria")
    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criarAgendamento(@RequestHeader("X-Usuario-Id") Long actorId,
                                               @Valid @RequestBody AgendamentoRequest request) {
        return agendamentos.criar(usuarios.buscar(actorId), request);
    }

    @Operation(summary = "Cancelar agendamento")
    @PatchMapping("/agendamentos/{id}/cancelamento")
    public AgendamentoResponse cancelarAgendamento(@RequestHeader("X-Usuario-Id") Long actorId,
                                                   @Parameter(description = "ID do agendamento") @PathVariable Long id) {
        return agendamentos.cancelar(usuarios.buscar(actorId), id);
    }
}
