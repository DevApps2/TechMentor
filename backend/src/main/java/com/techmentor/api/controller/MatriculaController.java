package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.MatriculaResponse;
import com.techmentor.api.service.MatriculaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matriculas")
@Tag(name = "Matrícula", description = "Matricular usuárias em cursos e listar matrículas")
public class MatriculaController {

    private final MatriculaService matriculaService;

    @Operation(summary = "Matricular usuária em um curso")
    @PostMapping("/{tituloCurso}/usuarios/{usuarioId}")
    public ResponseEntity<MatriculaResponse> matricular(@Parameter(description = "Título do curso") @PathVariable String tituloCurso,
                                                        @Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.status(201).body(matriculaService.matricularDTO(tituloCurso, usuarioId));
    }

    @Operation(summary = "Listar matrículas de uma usuária")
    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<MatriculaResponse>> listarMatriculas(@Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(matriculaService.listarMatriculasDTO(usuarioId));
    }
}
