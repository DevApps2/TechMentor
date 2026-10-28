package com.techmentor.api.controller;

import com.techmentor.api.dto.UsuarioDtos.CreateRequest;
import com.techmentor.api.dto.UsuarioDtos.LoginRequest;
import com.techmentor.api.dto.UsuarioDtos.Response;
import com.techmentor.api.dto.UsuarioDtos.Resumo;
import com.techmentor.api.dto.UsuarioDtos.UpdateRequest;
import com.techmentor.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/usuarios")
@Tag(name = "Usuário", description = "Cadastrar, criar, listar e deleter usuários")
public class UsuarioController {

    private final UsuarioService service;

    @Operation(summary = "Listar usuários")
    @GetMapping
    public ResponseEntity<List<Response>> listarUsuarios() {
        return ResponseEntity.ok(service.listarUsuarios());
    }

    @Operation(summary = "Buscar usuários pelo nome",
            description = "Usado para escolher quem denunciar ou bloquear. Retorna até 20 usuários ativos (sem e-mail); mínimo de 2 letras.")
    @GetMapping("/busca")
    public ResponseEntity<List<Resumo>> buscarPorNome(
            @Parameter(description = "Parte do nome") @RequestParam String nome,
            @Parameter(description = "ID de quem busca, para ele não aparecer no resultado")
            @RequestHeader(value = "X-Usuario-Id", required = false) Long solicitanteId) {
        return ResponseEntity.ok(service.buscarPorNome(nome, solicitanteId));
    }

    @Operation(summary = "Cadastrar usuário", description = "Cria a conta de uma nova usuária. Retorna 409 se o e-mail já estiver cadastrado.")
    @PostMapping
    public ResponseEntity<Response> cadastrar(@Valid @RequestBody CreateRequest dto) {
        return ResponseEntity.status(201).body(service.criarUsuario(dto));
    }

    @Operation(summary = "Entrar na plataforma", description = "Valida e-mail e senha e devolve os dados do usuário. Retorna 401 se as credenciais forem inválidas.")
    @PostMapping("/login")
    public ResponseEntity<Response> login(@Valid @RequestBody LoginRequest dto) {
        return ResponseEntity.ok(Response.from(service.loginUsuario(dto.email(), dto.senha())));
    }

    @Operation(summary = "Obter usuário por ID", description = "Dados do perfil, XP, nível e sequência. Retorna 404 se não existir ou estiver desativado.")
    @GetMapping("/{id}")
    public ResponseEntity<Response> obter(@Parameter(description = "ID do usuário") @PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza nome, e-mail e foto; bio e áreas de interesse só mudam se forem enviadas. Devolve o usuário atualizado.")
    @PutMapping("/{id}")
    public ResponseEntity<Response> atualizar(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Valid @RequestBody UpdateRequest dto) {

        return ResponseEntity.ok(service.atualizarUsuario(id, dto));
    }

    @Operation(summary = "Deletar usuário")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do usuário") @PathVariable Long id) {

        service.desativarUsuario(id);

        return ResponseEntity.status(200).build();
    }
}
