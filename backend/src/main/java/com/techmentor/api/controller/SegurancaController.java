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
    public DenunciaResponse denunciar(@Valid @RequestBody DenunciaRequest request) {
        return denuncias.criar(usuarios.buscarAtual(), request);
    }

    @Operation(summary = "Listar minhas denúncias")
    @GetMapping("/denuncias/minhas")
    public List<DenunciaResponse> minhasDenuncias() {
        return denuncias.minhas(usuarios.buscarAtual());
    }

    @Operation(summary = "Listar todas as denúncias")
    @GetMapping("/denuncias")
    public List<DenunciaResponse> listarDenuncias() {
        return denuncias.listarTodas(usuarios.buscarAtual());
    }

    @Operation(summary = "Atualizar a situação de uma denúncia",
            description = "Só para administradores. Muda o status (ABERTA, EM_ANALISE ou RESOLVIDA) e avisa quem denunciou por notificação.")
    @PatchMapping("/denuncias/{id}/status")
    public DenunciaResponse atualizarStatusDaDenuncia(@Parameter(description = "ID da denúncia") @PathVariable Long id,
                                                      @Valid @RequestBody DenunciaStatusRequest request) {
        return denuncias.atualizarStatus(usuarios.buscarAtual(), id, request.status());
    }

    @Operation(summary = "Obter denúncia por ID")
    @GetMapping("/denuncias/{id}")
    public DenunciaResponse obterDenuncia(@Parameter(description = "ID da denúncia") @PathVariable Long id) {
        return denuncias.obter(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Listar usuários bloqueados")
    @GetMapping("/bloqueios")
    public List<BloqueioResponse> listarBloqueios() {
        return bloqueios.listar(usuarios.buscarAtual());
    }

    @Operation(summary = "Bloquear usuário")
    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioResponse bloquear(@Valid @RequestBody BloqueioRequest request) {
        return bloqueios.bloquear(usuarios.buscarAtual(), request == null ? null : request.usuarioId());
    }

    @Operation(summary = "Desbloquear usuário")
    @DeleteMapping("/bloqueios/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desbloquear(@Parameter(description = "ID do usuário a desbloquear") @PathVariable Long usuarioId) {
        bloqueios.desbloquear(usuarios.buscarAtual(), usuarioId);
    }
}