package com.techmentor.api.controller;

import com.techmentor.api.dto.SegurancaDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.techmentor.api.model.ConteudoSeguranca;
import com.techmentor.api.service.BloqueioService;
import com.techmentor.api.service.ConteudoSegurancaService;
import com.techmentor.api.service.DenunciaService;
import com.techmentor.api.service.UsuarioAtualService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Segurança", description = "Denúncias e bloqueios de usuários")
public class SegurancaController {
    private final DenunciaService denuncias;
    private final BloqueioService bloqueios;
    private final UsuarioAtualService usuarios;
    private final ConteudoSegurancaService conteudos;

    public SegurancaController(DenunciaService denuncias, BloqueioService bloqueios, UsuarioAtualService usuarios,
                               ConteudoSegurancaService conteudos) {
        this.denuncias = denuncias;
        this.bloqueios = bloqueios;
        this.usuarios = usuarios;
        this.conteudos = conteudos;
    }

    @Operation(summary = "Listar orientações e canais de ajuda",
            description = "Conteúdo da tela de Segurança. Sem o filtro, devolve orientações e canais de ajuda.")
    @GetMapping("/seguranca/conteudos")
    public List<ConteudoSegurancaResponse> listarConteudos(
            @Parameter(description = "ORIENTACAO ou CANAL_AJUDA (opcional)") @RequestParam(required = false) ConteudoSeguranca.Tipo tipo) {
        return conteudos.listar(tipo);
    }

    @Operation(summary = "Registrar denúncia")
    @PostMapping("/denuncias")
    @ResponseStatus(HttpStatus.CREATED)
    public DenunciaResponse denunciar(@RequestHeader("X-Usuario-Id") Long actorId,
                                      @Valid @RequestBody DenunciaRequest request) {
        return denuncias.criar(usuarios.buscar(actorId), request);
    }

    @Operation(summary = "Listar minhas denúncias")
    @GetMapping("/denuncias/minhas")
    public List<DenunciaResponse> minhasDenuncias(@RequestHeader("X-Usuario-Id") Long actorId) {
        return denuncias.minhas(usuarios.buscar(actorId));
    }

    @Operation(summary = "Listar todas as denúncias")
    @GetMapping("/denuncias")
    public List<DenunciaResponse> listarDenuncias(@RequestHeader("X-Usuario-Id") Long actorId) {
        return denuncias.listarTodas(usuarios.buscar(actorId));
    }

    @Operation(summary = "Obter denúncia por ID")
    @GetMapping("/denuncias/{id}")
    public DenunciaResponse obterDenuncia(@RequestHeader("X-Usuario-Id") Long actorId,
                                          @Parameter(description = "ID da denúncia") @PathVariable Long id) {
        return denuncias.obter(usuarios.buscar(actorId), id);
    }

    @Operation(summary = "Listar usuários bloqueados")
    @GetMapping("/bloqueios")
    public List<BloqueioResponse> listarBloqueios(@RequestHeader("X-Usuario-Id") Long actorId) {
        return bloqueios.listar(usuarios.buscar(actorId));
    }

    @Operation(summary = "Bloquear usuário")
    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioResponse bloquear(@RequestHeader("X-Usuario-Id") Long actorId,
                                     @Valid @RequestBody BloqueioRequest request) {
        return bloqueios.bloquear(usuarios.buscar(actorId), request == null ? null : request.usuarioId());
    }

    @Operation(summary = "Desbloquear usuário")
    @DeleteMapping("/bloqueios/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desbloquear(@RequestHeader("X-Usuario-Id") Long actorId,
                            @Parameter(description = "ID do usuário a desbloquear") @PathVariable Long usuarioId) {
        bloqueios.desbloquear(usuarios.buscar(actorId), usuarioId);
    }
}
