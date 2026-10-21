package com.techmentor.api.controller;

import com.techmentor.api.dto.OportunidadeDtos.*;
import com.techmentor.api.service.OportunidadeService;
import com.techmentor.api.service.UsuarioAtualService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class OportunidadeController {
    private final OportunidadeService oportunidades;
    private final UsuarioAtualService usuarios;

    public OportunidadeController(OportunidadeService oportunidades, UsuarioAtualService usuarios) {
        this.oportunidades = oportunidades;
        this.usuarios = usuarios;
    }

    @GetMapping("/oportunidades")
    public List<OportunidadeResponse> listar() { return oportunidades.listar(); }

    @GetMapping("/oportunidades/{id}")
    public OportunidadeResponse obter(@PathVariable Long id) { return oportunidades.obter(id); }

    @PostMapping("/oportunidades")
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadeResponse criar(@RequestHeader("X-Usuario-Id") Long actorId,
                                      @RequestBody OportunidadeRequest request) {
        return oportunidades.criar(usuarios.buscar(actorId), request);
    }

    @PutMapping("/oportunidades/{id}")
    public OportunidadeResponse atualizar(@RequestHeader("X-Usuario-Id") Long actorId,
                                          @PathVariable Long id,
                                          @RequestBody OportunidadeRequest request) {
        return oportunidades.atualizar(usuarios.buscar(actorId), id, request);
    }

    @DeleteMapping("/oportunidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id) {
        oportunidades.excluir(usuarios.buscar(actorId), id);
    }

    @PostMapping("/oportunidades/{id}/candidaturas")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse candidatar(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id) {
        return oportunidades.candidatar(usuarios.buscar(actorId), id);
    }

    @PostMapping("/oportunidades/{id}/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse inscrever(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id) {
        return oportunidades.inscreverEmEvento(usuarios.buscar(actorId), id);
    }

    @GetMapping("/candidaturas/minhas")
    public List<InscricaoResponse> minhasCandidaturas(@RequestHeader("X-Usuario-Id") Long actorId) {
        return oportunidades.minhasInscricoes(usuarios.buscar(actorId), false);
    }

    @GetMapping("/inscricoes/minhas")
    public List<InscricaoResponse> minhasInscricoes(@RequestHeader("X-Usuario-Id") Long actorId) {
        return oportunidades.minhasInscricoes(usuarios.buscar(actorId), true);
    }

    @GetMapping("/candidaturas/{id}")
    public InscricaoResponse obterCandidatura(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id) {
        return oportunidades.obterCandidatura(usuarios.buscar(actorId), id);
    }
}
