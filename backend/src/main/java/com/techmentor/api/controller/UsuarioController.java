package com.techmentor.api.controller;

import com.techmentor.api.dto.UsuarioDtos.CreateRequest;
import com.techmentor.api.dto.UsuarioDtos.Response;
import com.techmentor.api.dto.UsuarioDtos.UpdateRequest;
import com.techmentor.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    public ResponseEntity<List<Response>> listarUsuarios() {
        return ResponseEntity.ok(service.listarUsuarios());
    }

    @PostMapping
    public ResponseEntity<Response> cadastrar(
            @Valid @RequestBody CreateRequest dto) {

        Response resposta = service.criarUsuario(dto);

        return ResponseEntity.status(201).body(resposta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequest dto) {

        service.atualizarUsuario(id, dto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {

        service.deletarUsuario(id);

        return ResponseEntity.noContent().build();
    }
}
