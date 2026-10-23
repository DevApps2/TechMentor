package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.DesafioRequest;
import com.techmentor.api.dto.GamificacaoDtos.DesafioResponse;
import com.techmentor.api.dto.GamificacaoDtos.UsuarioDesafioResponse;
import com.techmentor.api.service.DesafioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/desafios")
public class DesafioController {

    private final DesafioService desafioService;

    @PostMapping
    public ResponseEntity<DesafioResponse> criarDesafio(@Valid @RequestBody DesafioRequest request) {
        return ResponseEntity.status(201).body(desafioService.criarDesafio(request));
    }

    @GetMapping
    public ResponseEntity<List<DesafioResponse>> listarDesafiosAtivos() {
        return ResponseEntity.ok(desafioService.listarDesafiosAtivosDTO());
    }

    @PostMapping("/{titulo}/participacao/usuarios/{usuarioId}")
    public ResponseEntity<UsuarioDesafioResponse> participar(@PathVariable String titulo, @PathVariable Long usuarioId) {
        return ResponseEntity.status(201).body(desafioService.participarDTO(titulo, usuarioId));
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<UsuarioDesafioResponse>> listarDesafiosDaUsuaria(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(desafioService.listarDesafiosDaUsuariaDTO(usuarioId));
    }

    @PutMapping("/{titulo}")
    public ResponseEntity<DesafioResponse> atualizarDesafio(@PathVariable String titulo,
                                                            @Valid @RequestBody DesafioRequest request) {
        return ResponseEntity.ok(desafioService.atualizarDesafio(titulo, request));
    }

    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarDesafio(@PathVariable String titulo) {
        desafioService.deletarDesafio(titulo);
        return ResponseEntity.noContent().build();
    }
}
