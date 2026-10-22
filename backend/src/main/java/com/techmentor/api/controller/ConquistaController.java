package com.techmentor.api.controller;

import com.techmentor.api.model.Conquista;
import com.techmentor.api.model.UsuarioConquista;
import com.techmentor.api.service.ConquistaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conquistas")
public class ConquistaController {

    private final ConquistaService conquistaService;

    @PostMapping
    public ResponseEntity<Conquista> criarConquista(@RequestBody Conquista conquista) {
        return ResponseEntity.status(201).body(conquistaService.criarConquista(conquista));
    }

    @GetMapping
    public ResponseEntity<List<Conquista>> listarConquistas() {
        return ResponseEntity.ok(conquistaService.listarConquistas());
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<UsuarioConquista>> listarConquistasDaUsuaria(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(conquistaService.listarConquistasDaUsuaria(usuarioId));
    }

    @PutMapping("/{nome}")
    public ResponseEntity<Conquista> atualizarConquista(@PathVariable String nome, @RequestBody Conquista dados) {
        return ResponseEntity.ok(conquistaService.atualizarConquista(nome, dados));
    }

    @DeleteMapping("/{nome}")
    public ResponseEntity<Void> deletarConquista(@PathVariable String nome) {
        conquistaService.deletarConquista(nome);
        return ResponseEntity.noContent().build();
    }
}