package com.techmentor.api.controller;

import com.techmentor.api.dto.OportunidadeDtos.*;
import com.techmentor.api.model.Oportunidade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.techmentor.api.service.OportunidadeService;
import com.techmentor.api.service.UsuarioAtualService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Oportunidade", description = "Oportunidades, eventos, candidaturas e inscrições")
public class OportunidadeController {
    private final OportunidadeService oportunidades;
    private final UsuarioAtualService usuarios;

    public OportunidadeController(OportunidadeService oportunidades, UsuarioAtualService usuarios) {
        this.oportunidades = oportunidades;
        this.usuarios = usuarios;
    }

    @Operation(summary = "Listar oportunidades", description = "Oportunidades ativas e com prazo de inscrição em aberto (as vencidas não aparecem), com o prazo mais próximo primeiro. Filtros opcionais por tipo e por área.")
    @GetMapping("/oportunidades")
    public List<OportunidadeResponse> listar(
            @Parameter(description = "Tipo: ESTAGIO, EMPREGO, BOLSA ou EVENTO (opcional)") @RequestParam(required = false) Oportunidade.Tipo tipo,
            @Parameter(description = "Área, por exemplo Programação (opcional)") @RequestParam(required = false) String area,
            @Parameter(description = "Texto para buscar no título, na organização, na área e na descrição (opcional)") @RequestParam(required = false) String busca) {
        return oportunidades.listar(tipo, area, busca);
    }

    @Operation(summary = "Obter oportunidade por ID")
    @GetMapping("/oportunidades/{id}")
    public OportunidadeResponse obter(@Parameter(description = "ID da oportunidade") @PathVariable Long id) { return oportunidades.obter(id); }

    @Operation(summary = "Criar oportunidade")
    @PostMapping("/oportunidades")
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadeResponse criar(@Valid @RequestBody OportunidadeRequest request) {
        return oportunidades.criar(usuarios.buscarAtual(), request);
    }

    @Operation(summary = "Atualizar oportunidade")
    @PutMapping("/oportunidades/{id}")
    public OportunidadeResponse atualizar(@Parameter(description = "ID da oportunidade") @PathVariable Long id,
                                          @Valid @RequestBody OportunidadeRequest request) {
        return oportunidades.atualizar(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Excluir oportunidade")
    @DeleteMapping("/oportunidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@Parameter(description = "ID da oportunidade") @PathVariable Long id) {
        oportunidades.excluir(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Candidatar-se a uma oportunidade")
    @PostMapping("/oportunidades/{id}/candidaturas")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse candidatar(@Parameter(description = "ID da oportunidade") @PathVariable Long id) {
        return oportunidades.candidatar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Inscrever-se em um evento")
    @PostMapping("/oportunidades/{id}/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse inscrever(@Parameter(description = "ID da oportunidade (evento)") @PathVariable Long id) {
        return oportunidades.inscreverEmEvento(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Listar minhas candidaturas")
    @GetMapping("/candidaturas/minhas")
    public List<InscricaoResponse> minhasCandidaturas() {
        return oportunidades.minhasInscricoes(usuarios.buscarAtual(), false);
    }

    @Operation(summary = "Listar minhas inscrições em eventos")
    @GetMapping("/inscricoes/minhas")
    public List<InscricaoResponse> minhasInscricoes() {
        return oportunidades.minhasInscricoes(usuarios.buscarAtual(), true);
    }

    @Operation(summary = "Obter candidatura por ID")
    @GetMapping("/candidaturas/{id}")
    public InscricaoResponse obterCandidatura(@Parameter(description = "ID da candidatura") @PathVariable Long id) {
        return oportunidades.obterCandidatura(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Retirar candidatura ou inscrição",
            description = "A própria usuária desiste enquanto a equipe ainda não respondeu. Retorna 409 se já foi aprovada ou recusada.")
    @DeleteMapping("/candidaturas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desistir(@Parameter(description = "ID da candidatura ou inscrição") @PathVariable Long id) {
        oportunidades.desistir(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Candidaturas de uma oportunidade", description = "Só para administradoras.")
    @GetMapping("/admin/oportunidades/{id}/candidaturas")
    public List<CandidaturaAdminResponse> candidaturasDaOportunidade(
            @Parameter(description = "ID da oportunidade") @PathVariable Long id) {
        return oportunidades.candidaturasDaOportunidade(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Mudar a situação de uma candidatura",
            description = "Só para administradoras. ENVIADA, EM_ANALISE, APROVADA ou RECUSADA; avisa quem se candidatou por notificação.")
    @PatchMapping("/admin/candidaturas/{id}/status")
    public CandidaturaAdminResponse atualizarStatusDaCandidatura(
            @Parameter(description = "ID da candidatura") @PathVariable Long id,
            @Valid @RequestBody CandidaturaStatusRequest request) {
        return oportunidades.atualizarStatus(usuarios.buscarAtual(), id, request.status());
    }
}
