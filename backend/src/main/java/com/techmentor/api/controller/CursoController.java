package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.model.Curso;
import com.techmentor.api.service.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public ResponseEntity<List<CursoResponse>> listarCursos(@RequestParam(required = false) Curso.Categoria categoria) {
        return ResponseEntity.ok(cursoService.listarCursosDTO(categoria));
    }

    @GetMapping("/desativados")
    public ResponseEntity<List<CursoResponse>> listarCursosDesativados() {
        return ResponseEntity.ok(cursoService.listarCursosDesativadosDTO());
    }

    @GetMapping("/buscar")
    public ResponseEntity<CursoResponse> buscarPorTitulo(@RequestParam String titulo) {
        return ResponseEntity.ok(cursoService.detalharCursoDTO(titulo));
    }

    @PostMapping
    public ResponseEntity<CursoResponse> cadastrarCurso(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(201).body(cursoService.criarCurso(request));
    }

    @PutMapping("/{titulo}")
    public ResponseEntity<CursoResponse> atualizarCurso(@PathVariable String titulo,
                                                        @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(cursoService.atualizarCurso(titulo, request));
    }

    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarCurso(@PathVariable String titulo) {
        cursoService.deletarCurso(titulo);
        return ResponseEntity.noContent().build();
    }
}
