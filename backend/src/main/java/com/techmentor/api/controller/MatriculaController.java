package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.MatriculaResponse;
import com.techmentor.api.service.MatriculaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matriculas")
public class MatriculaController {

    private final MatriculaService matriculaService;

    @PostMapping("/{tituloCurso}/usuarios/{usuarioId}")
    public ResponseEntity<MatriculaResponse> matricular(@PathVariable String tituloCurso,
                                                        @PathVariable Long usuarioId) {
        return ResponseEntity.status(201).body(matriculaService.matricularDTO(tituloCurso, usuarioId));
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<MatriculaResponse>> listarMatriculas(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(matriculaService.listarMatriculasDTO(usuarioId));
    }
}
