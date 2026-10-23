package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.AulaRequest;
import com.techmentor.api.dto.CursoDtos.AulaResponse;
import jakarta.validation.Valid;
import com.techmentor.api.service.AulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/aulas")
public class AulaController {
    private final AulaService aulaService;

    @PostMapping("/{titulo}/{ordemModulo}")
    public ResponseEntity<AulaResponse> criarAula(@PathVariable String titulo,
                                                  @PathVariable int ordemModulo,
                                                  @Valid @RequestBody AulaRequest request) {
        return ResponseEntity.status(201).body(aulaService.criarAula(titulo, ordemModulo, request));
    }

    @DeleteMapping("/{titulo}/{ordemModulo}/{ordemAula}")
    public ResponseEntity<Void> deletarAula(@PathVariable String titulo, @PathVariable int ordemModulo, @PathVariable int ordemAula) {
        aulaService.deletarAula(titulo, ordemModulo, ordemAula);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{titulo}/{ordemModulo}/{ordemAula}")
    public ResponseEntity<AulaResponse> atualizarAula(@PathVariable String titulo, @PathVariable int ordemModulo,
                                                      @PathVariable int ordemAula,
                                                      @Valid @RequestBody AulaRequest request) {
        return ResponseEntity.ok(aulaService.atualizarAula(titulo, ordemModulo, ordemAula, request));
    }
}
