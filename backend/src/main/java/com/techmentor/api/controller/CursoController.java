package com.techmentor.api.controller;

import com.techmentor.api.model.Curso;
import com.techmentor.api.service.CursoService;
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
    public ResponseEntity<List<Curso>> listarCursosAtivos() {
        return ResponseEntity.ok(cursoService.listarCursosAtivos());
    }

    @GetMapping("/desativados")
    public ResponseEntity<List<Curso>> listarCursosDesativados() {
        return ResponseEntity.ok(cursoService.listarCursosDesativados());
    }

    @GetMapping("/buscar")
    public ResponseEntity<Curso> buscarPorTitulo(@RequestParam String titulo) {
        return ResponseEntity.ok(cursoService.buscarPorTitulo(titulo));
    }

    @PostMapping
    public ResponseEntity<Curso> cadastrarCurso(@RequestBody Curso curso) {
        return ResponseEntity.status(201).body(cursoService.criarCurso(curso));
    }

    @PutMapping("/{titulo}")
    public ResponseEntity<Void> atualizarCurso(@PathVariable String titulo, @RequestBody Curso curso) {
        cursoService.atualizarCurso(titulo, curso);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarCurso(@PathVariable String titulo) {
        cursoService.deletarCurso(titulo);
        return ResponseEntity.noContent().build();
    }
}