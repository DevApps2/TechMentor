package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.DesafioRequest;
import com.techmentor.api.dto.GamificacaoDtos.DesafioResponse;
import com.techmentor.api.dto.GamificacaoDtos.UsuarioDesafioResponse;
import com.techmentor.api.service.DesafioService;
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
@RequestMapping("/desafios")
@Tag(name = "Desafio", description = "Gerenciar desafios e a participação das usuárias")
public class DesafioController {

    private final DesafioService desafioService;

    @Operation(summary = "Criar desafio")
    @PostMapping
    public ResponseEntity<DesafioResponse> criarDesafio(@Valid @RequestBody DesafioRequest request) {
        return ResponseEntity.status(201).body(desafioService.criarDesafio(request));
    }

    @Operation(summary = "Listar desafios ativos")
    @GetMapping
    public ResponseEntity<List<DesafioResponse>> listarDesafiosAtivos() {
        return ResponseEntity.ok(desafioService.listarDesafiosAtivosDTO());
    }

    @Operation(summary = "Participar de um desafio")
    @PostMapping("/{titulo}/participacao/usuarios/{usuarioId}")
    public ResponseEntity<UsuarioDesafioResponse> participar(@Parameter(description = "Título do desafio") @PathVariable String titulo,
                                                             @Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.status(201).body(desafioService.participarDTO(titulo, usuarioId));
    }

    @Operation(summary = "Listar desafios de uma usuária")
    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<UsuarioDesafioResponse>> listarDesafiosDaUsuaria(
            @Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(desafioService.listarDesafiosDaUsuariaDTO(usuarioId));
    }

    @Operation(summary = "Atualizar desafio")
    @PutMapping("/{titulo}")
    public ResponseEntity<DesafioResponse> atualizarDesafio(@Parameter(description = "Título do desafio") @PathVariable String titulo,
                                                            @Valid @RequestBody DesafioRequest request) {
        return ResponseEntity.ok(desafioService.atualizarDesafio(titulo, request));
    }

    @Operation(summary = "Deletar desafio")
    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarDesafio(@Parameter(description = "Título do desafio") @PathVariable String titulo) {
        desafioService.deletarDesafio(titulo);
        return ResponseEntity.noContent().build();
    }
}
