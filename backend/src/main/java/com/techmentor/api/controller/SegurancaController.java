package com.techmentor.api.controller;

import com.techmentor.api.dto.SegurancaDtos.*;
import com.techmentor.api.service.BloqueioService;
import com.techmentor.api.service.DenunciaService;
import com.techmentor.api.service.UsuarioAtualService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SegurancaController {
    private final DenunciaService denuncias;
    private final BloqueioService bloqueios;
    private final UsuarioAtualService usuarios;

    public SegurancaController(DenunciaService denuncias, BloqueioService bloqueios, UsuarioAtualService usuarios) {
        this.denuncias = denuncias;
        this.bloqueios = bloqueios;
        this.usuarios = usuarios;
    }

    @PostMapping("/denuncias")
    @ResponseStatus(HttpStatus.CREATED)
    public DenunciaResponse denunciar(@RequestHeader("X-Usuario-Id") Long actorId,
                                      @RequestBody DenunciaRequest request) {
        return denuncias.criar(usuarios.buscar(actorId), request);
    }

    @GetMapping("/denuncias/minhas")
    public List<DenunciaResponse> minhasDenuncias(@RequestHeader("X-Usuario-Id") Long actorId) {
        return denuncias.minhas(usuarios.buscar(actorId));
    }

    @GetMapping("/denuncias")
    public List<DenunciaResponse> listarDenuncias(@RequestHeader("X-Usuario-Id") Long actorId) {
        return denuncias.listarTodas(usuarios.buscar(actorId));
    }

    @GetMapping("/denuncias/{id}")
    public DenunciaResponse obterDenuncia(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long id) {
        return denuncias.obter(usuarios.buscar(actorId), id);
    }

    @GetMapping("/bloqueios")
    public List<BloqueioResponse> listarBloqueios(@RequestHeader("X-Usuario-Id") Long actorId) {
        return bloqueios.listar(usuarios.buscar(actorId));
    }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioResponse bloquear(@RequestHeader("X-Usuario-Id") Long actorId,
                                     @RequestBody BloqueioRequest request) {
        return bloqueios.bloquear(usuarios.buscar(actorId), request == null ? null : request.usuarioId());
    }

    @DeleteMapping("/bloqueios/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desbloquear(@RequestHeader("X-Usuario-Id") Long actorId, @PathVariable Long usuarioId) {
        bloqueios.desbloquear(usuarios.buscar(actorId), usuarioId);
    }
}
