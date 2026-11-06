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
    public AreaResponse criarArea(@Valid @RequestBody AreaRequest request) {
        return mentoras.criarArea(usuarios.buscarAtual(), request == null ? null : request.nome());
    }

    @Operation(summary = "Listar mentoras", description = "Busca opcional por nome, cargo ou área, e filtro por uma área (nome exato).")
    @GetMapping("/mentoras")
    public List<MentoraResponse> listarMentoras(
            @Parameter(description = "Texto para buscar no nome, cargo ou áreas (opcional)") @RequestParam(required = false) String busca,
            @Parameter(description = "Nome exato de uma área de atuação (opcional)") @RequestParam(required = false) String area) {
        return mentoras.listarMentoras(busca, area);
    }

    @Operation(summary = "Meu perfil de mentora",
            description = "Perfil da mentora logada. Retorna 404 se ela ainda não preencheu o perfil, e 403 se o usuário não é mentora.")
    @GetMapping("/mentoras/minha")
    public MentoraResponse minhaMentora() {
        return mentoras.minhaMentora(usuarios.buscarAtual());
    }
    @Operation(summary = "Obter mentora por ID")
    @GetMapping("/mentoras/{id}")
    public MentoraResponse obterMentora(@Parameter(description = "ID da mentora") @PathVariable Long id) { return mentoras.obterMentora(id); }

    @Operation(summary = "Criar perfil de mentora")
    @PostMapping("/mentoras")
    @ResponseStatus(HttpStatus.CREATED)
    public MentoraResponse criarMentora(@Valid @RequestBody MentoraRequest request) {
        return mentoras.criarMentora(usuarios.buscarAtual(), request);
    }

    @Operation(summary = "Atualizar perfil de mentora")
    @PutMapping("/mentoras/{id}")
    public MentoraResponse atualizarMentora(@Parameter(description = "ID da mentora") @PathVariable Long id,
                                            @Valid @RequestBody MentoraRequest request) {
        return mentoras.atualizarMentora(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Excluir perfil de mentora")
    @DeleteMapping("/mentoras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirMentora(@Parameter(description = "ID da mentora") @PathVariable Long id) {
        mentoras.excluirMentora(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Listar disponibilidades de uma mentora")
    @GetMapping("/mentoras/{id}/disponibilidades")
    public List<HorarioResponse> listarHorarios(@Parameter(description = "ID da mentora") @PathVariable Long id) {
        return mentoras.listarHorarios(id);
    }

    @Operation(summary = "Criar disponibilidade de uma mentora")
    @PostMapping("/mentoras/{id}/disponibilidades")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioResponse criarHorario(@Parameter(description = "ID da mentora") @PathVariable Long id,
                                        @Valid @RequestBody HorarioRequest request) {
        return mentoras.criarHorario(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Excluir disponibilidade")
    @DeleteMapping("/disponibilidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirHorario(@Parameter(description = "ID da disponibilidade") @PathVariable Long id) {
        mentoras.excluirHorario(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Listar agendamentos do usuário")
    @GetMapping("/agendamentos")
    public List<AgendamentoResponse> listarAgendamentos() {
        return agendamentos.listar(usuarios.buscarAtual());
    }

    @Operation(summary = "Obter agendamento por ID")
    @GetMapping("/agendamentos/{id}")
    public AgendamentoResponse obterAgendamento(@Parameter(description = "ID do agendamento") @PathVariable Long id) {
        return agendamentos.obter(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Criar agendamento de mentoria")
    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criarAgendamento(@Valid @RequestBody AgendamentoRequest request) {
        return agendamentos.criar(usuarios.buscarAtual(), request);
    }

    @Operation(summary = "Cancelar agendamento")
    @PatchMapping("/agendamentos/{id}/cancelamento")
    public AgendamentoResponse cancelarAgendamento(@Parameter(description = "ID do agendamento") @PathVariable Long id) {
        return agendamentos.cancelar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Confirmar agendamento",
            description = "Só a mentora da mentoria (ou admin). Passa de AGENDADA para CONFIRMADA e avisa a aluna por notificação.")
    @PatchMapping("/agendamentos/{id}/confirmacao")
    public AgendamentoResponse confirmarAgendamento(@Parameter(description = "ID do agendamento") @PathVariable Long id) {
        return agendamentos.confirmar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Reagendar mentoria",
            description = "A aluna ou a mentora da mentoria (ou admin) troca por outro horário livre da mesma mentora. "
                    + "Se a aluna reagenda, volta para AGENDADA e a mentora confirma de novo; se a mentora reagenda, fica CONFIRMADA. "
                    + "A outra pessoa recebe uma notificação.")
    @PatchMapping("/agendamentos/{id}/reagendamento")
    public AgendamentoResponse reagendarAgendamento(@Parameter(description = "ID do agendamento") @PathVariable Long id,
                                                    @Valid @RequestBody ReagendamentoRequest request) {
        return agendamentos.reagendar(usuarios.buscarAtual(), id, request.horarioId());
    }
}
