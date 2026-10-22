package com.techmentor.api.controller;

import com.techmentor.api.model.Desafio;
import com.techmentor.api.model.UsuarioDesafio;
import com.techmentor.api.service.DesafioService;
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
    public ResponseEntity<Desafio> criarDesafio(@RequestBody Desafio desafio) {
        return ResponseEntity.status(201).body(desafioService.criarDesafio(desafio));
    }

    @GetMapping
    public ResponseEntity<List<Desafio>> listarDesafiosAtivos() {
        return ResponseEntity.ok(desafioService.listarDesafiosAtivos());
    }

    @PostMapping("/{titulo}/participacao/usuarios/{usuarioId}")
    public ResponseEntity<UsuarioDesafio> participar(@PathVariable String titulo, @PathVariable Long usuarioId) {
        return ResponseEntity.status(201).body(desafioService.participar(titulo, usuarioId));
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<UsuarioDesafio>> listarDesafiosDaUsuaria(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(desafioService.listarDesafiosDaUsuaria(usuarioId));
    }

    @PutMapping("/{titulo}")
    public ResponseEntity<Desafio> atualizarDesafio(@PathVariable String titulo, @RequestBody Desafio dados) {
        return ResponseEntity.ok(desafioService.atualizarDesafio(titulo, dados));
    }

    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarDesafio(@PathVariable String titulo) {
        desafioService.deletarDesafio(titulo);
        return ResponseEntity.noContent().build();
    }
}
