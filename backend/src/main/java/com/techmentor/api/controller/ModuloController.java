package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.ModuloRequest;
import com.techmentor.api.dto.CursoDtos.ModuloDeleteRequest;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import jakarta.validation.Valid;
import com.techmentor.api.service.ModuloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/modulos")
public class ModuloController {

    private final ModuloService moduloService;

    @PostMapping("/{titulo}")
    public ResponseEntity<ModuloResponse> criarModulo(@PathVariable String titulo,
                                                      @Valid @RequestBody ModuloRequest request) {
        return ResponseEntity.status(201).body(moduloService.criarModulo(titulo, request));
    }

    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarModuloLegado(@PathVariable String titulo,
                                                    @Valid @RequestBody ModuloDeleteRequest request) {
        moduloService.deletarModulo(titulo, request.ordem());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{titulo}/{ordem}")
    public ResponseEntity<Void> deletarModulo(@PathVariable String titulo, @PathVariable int ordem) {
        moduloService.deletarModulo(titulo, ordem);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{titulo}/{ordem}")
    public ResponseEntity<ModuloResponse> atualizarModulo(@PathVariable String titulo, @PathVariable int ordem,
                                                          @Valid @RequestBody ModuloRequest request) {
        return ResponseEntity.ok(moduloService.atualizarModulo(titulo, ordem, request));
    }
}
