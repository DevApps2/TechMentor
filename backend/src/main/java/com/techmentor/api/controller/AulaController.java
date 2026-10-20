package com.techmentor.api.controller;

import com.techmentor.api.model.Aula;
import com.techmentor.api.service.AulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/aulas")
public class AulaController {
    private final AulaService aulaService;

    @PostMapping("/{titulo}/{ordemMudolo}")
    public ResponseEntity<Aula> criarAula(@PathVariable String titulo, @RequestBody Aula aula, @PathVariable int ordemMudolo) {
        this.aulaService.CriarAula(titulo, ordemMudolo, aula);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{titulo}/{ordemModulo}/{ordemAula}")
    public ResponseEntity<Void> deletarAula(@PathVariable String titulo, @PathVariable int ordemModulo, @PathVariable int ordemAula) {
        aulaService.deletarAula(titulo, ordemModulo, ordemAula);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{titulo}/{ordemModulo}/{ordemAula}")
    public ResponseEntity<Aula> atualizarAula(@PathVariable String titulo, @PathVariable int ordemModulo,
                                              @PathVariable int ordemAula, @RequestBody Aula aula) {
        return ResponseEntity.ok(aulaService.atualizarAula(titulo, ordemModulo, ordemAula, aula));
    }
}
