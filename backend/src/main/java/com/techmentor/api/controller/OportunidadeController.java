package com.techmentor.api.controller;

import com.techmentor.api.dto.OportunidadeDtos.*;
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

    @Operation(summary = "Listar oportunidades")
    @GetMapping("/oportunidades")
    public List<OportunidadeResponse> listar() { return oportunidades.listar(); }

    @Operation(summary = "Obter oportunidade por ID")
    @GetMapping("/oportunidades/{id}")
    public OportunidadeResponse obter(@Parameter(description = "ID da oportunidade") @PathVariable Long id) { return oportunidades.obter(id); }

    @Operation(summary = "Criar oportunidade")
    @PostMapping("/oportunidades")
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadeResponse criar(@RequestHeader("X-Usuario-Id") Long actorId,
                                      @Valid @RequestBody OportunidadeRequest request) {
        return oportunidades.criar(usuarios.buscar(actorId), request);
    }

    @Operation(summary = "Atualizar oportunidade")
    @PutMapping("/oportunidades/{id}")
    public OportunidadeResponse atualizar(@RequestHeader("X-Usuario-Id") Long actorId,
                                          @Parameter(description = "ID da oportunidade") @PathVariable Long id,
                                          @Valid @RequestBody OportunidadeRequest request) {
        return oportunidades.atualizar(usuarios.buscar(actorId), id, request);
    }

    @Operation(summary = "Excluir oportunidade")
    @DeleteMapping("/oportunidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@RequestHeader("X-Usuario-Id") Long actorId,
                        @Parameter(description = "ID da oportunidade") @PathVariable Long id) {
        oportunidades.excluir(usuarios.buscar(actorId), id);
    }

    @Operation(summary = "Candidatar-se a uma oportunidade")
    @PostMapping("/oportunidades/{id}/candidaturas")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse candidatar(@RequestHeader("X-Usuario-Id") Long actorId,
                                        @Parameter(description = "ID da oportunidade") @PathVariable Long id) {
        return oportunidades.candidatar(usuarios.buscar(actorId), id);
    }

    @Operation(summary = "Inscrever-se em um evento")
    @PostMapping("/oportunidades/{id}/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse inscrever(@RequestHeader("X-Usuario-Id") Long actorId,
                                       @Parameter(description = "ID da oportunidade (evento)") @PathVariable Long id) {
        return oportunidades.inscreverEmEvento(usuarios.buscar(actorId), id);
    }

    @Operation(summary = "Listar minhas candidaturas")
    @GetMapping("/candidaturas/minhas")
    public List<InscricaoResponse> minhasCandidaturas(@RequestHeader("X-Usuario-Id") Long actorId) {
        return oportunidades.minhasInscricoes(usuarios.buscar(actorId), false);
    }

    @Operation(summary = "Listar minhas inscrições em eventos")
    @GetMapping("/inscricoes/minhas")
    public List<InscricaoResponse> minhasInscricoes(@RequestHeader("X-Usuario-Id") Long actorId) {
        return oportunidades.minhasInscricoes(usuarios.buscar(actorId), true);
    }

    @Operation(summary = "Obter candidatura por ID")
    @GetMapping("/candidaturas/{id}")
    public InscricaoResponse obterCandidatura(@RequestHeader("X-Usuario-Id") Long actorId,
                                              @Parameter(description = "ID da candidatura") @PathVariable Long id) {
        return oportunidades.obterCandidatura(usuarios.buscar(actorId), id);
    }
}
