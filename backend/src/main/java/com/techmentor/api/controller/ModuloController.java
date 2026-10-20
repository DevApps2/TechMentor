package com.techmentor.api.controller;

import com.techmentor.api.model.Modulo;
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
    public ResponseEntity<Modulo> criarModulo(@PathVariable String titulo, @RequestBody Modulo mudolo){
        this.moduloService.criarModulo(titulo,mudolo);
        return  ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarModulo(@PathVariable String titulo, @RequestBody Modulo modulo) {
        moduloService.deletarModulo(titulo, modulo.getOrdem());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{titulo}/{ordem}")
    public ResponseEntity<Modulo> atualizarModulo(@PathVariable String titulo, @PathVariable int ordem, @RequestBody Modulo modulo) {
        this.moduloService.atualizarModulo(titulo, ordem, modulo) ;
        return ResponseEntity.status(200).build();
    }
}