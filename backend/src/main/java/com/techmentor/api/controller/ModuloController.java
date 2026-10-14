package com.techmentor.api.controller;

import com.techmentor.api.model.Modulo;
import com.techmentor.api.service.ModuloService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/cursos/{titulo}/modulos")
public class ModuloController {

    private final ModuloService moduloService;

    public ModuloController(ModuloService moduloService) {
        this.moduloService = moduloService;
    }

    @GetMapping("/{ordem}")
    public ResponseEntity<Modulo> buscarModulo(@PathVariable String titulo, @PathVariable int ordem) {
        return ResponseEntity.ok(moduloService.buscarModulo(titulo, ordem));
    }
}