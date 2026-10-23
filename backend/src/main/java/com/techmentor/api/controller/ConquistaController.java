package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.ConquistaRequest;
import com.techmentor.api.dto.GamificacaoDtos.ConquistaResponse;
import com.techmentor.api.dto.GamificacaoDtos.UsuarioConquistaResponse;
import com.techmentor.api.service.ConquistaService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ConquistaResponse> criarConquista(@Valid @RequestBody ConquistaRequest request) {
        return ResponseEntity.status(201).body(conquistaService.criarConquista(request));
    }

    @GetMapping
    public ResponseEntity<List<ConquistaResponse>> listarConquistas() {
        return ResponseEntity.ok(conquistaService.listarConquistasDTO());
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<UsuarioConquistaResponse>> listarConquistasDaUsuaria(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(conquistaService.listarConquistasDaUsuariaDTO(usuarioId));
    }

    @PutMapping("/{nome}")
    public ResponseEntity<ConquistaResponse> atualizarConquista(@PathVariable String nome,
                                                                @Valid @RequestBody ConquistaRequest request) {
        return ResponseEntity.ok(conquistaService.atualizarConquista(nome, request));
    }

    @DeleteMapping("/{nome}")
    public ResponseEntity<Void> deletarConquista(@PathVariable String nome) {
        conquistaService.deletarConquista(nome);
        return ResponseEntity.noContent().build();
    }
}
