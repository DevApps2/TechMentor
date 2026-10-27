package com.techmentor.api.controller;

import com.techmentor.api.dto.UsuarioDtos.CreateRequest;
import com.techmentor.api.dto.UsuarioDtos.Response;
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

    @Operation(summary = "Cadastrar usuário")
    @PostMapping
    public ResponseEntity<Response> cadastrar(
            @Valid @RequestBody CreateRequest dto) {

        Response resposta = service.criarUsuario(dto);

        return ResponseEntity.status(201).body(resposta);
    }

    @Operation(summary = "Atualizar usuário")
    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Valid @RequestBody UpdateRequest dto) {

        service.atualizarUsuario(id, dto);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deletar usuário")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do usuário") @PathVariable Long id) {

        service.desativarUsuario(id);

        return ResponseEntity.status(200).build();
    }
}
