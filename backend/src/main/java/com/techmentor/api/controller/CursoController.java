package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.model.Curso;
import com.techmentor.api.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cursos")
@Tag(name = "Curso", description = "Cadastrar, listar, buscar, atualizar e deletar cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @Operation(summary = "Listar cursos", description = "Lista os cursos ativos, com filtro opcional por categoria.")
    @GetMapping
    public ResponseEntity<List<CursoResponse>> listarCursos(
            @Parameter(description = "Categoria do curso (opcional)") @RequestParam(required = false) Curso.Categoria categoria) {
        return ResponseEntity.ok(cursoService.listarCursosDTO(categoria));
    }

    @Operation(summary = "Listar cursos desativados")
    @GetMapping("/desativados")
    public ResponseEntity<List<CursoResponse>> listarCursosDesativados() {
        return ResponseEntity.ok(cursoService.listarCursosDesativadosDTO());
    }

    @Operation(summary = "Buscar curso por título")
    @GetMapping("/buscar")
    public ResponseEntity<CursoResponse> buscarPorTitulo(@Parameter(description = "Título do curso") @RequestParam String titulo) {
        return ResponseEntity.ok(cursoService.detalharCursoDTO(titulo));
    }

    @Operation(summary = "Cadastrar curso")
    @PostMapping
    public ResponseEntity<CursoResponse> cadastrarCurso(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(201).body(cursoService.criarCurso(request));
    }

    @Operation(summary = "Atualizar curso")
    @PutMapping("/{titulo}")
    public ResponseEntity<CursoResponse> atualizarCurso(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                        @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(cursoService.atualizarCurso(titulo, request));
    }

    @Operation(summary = "Deletar curso")
    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarCurso(@Parameter(description = "Título do curso") @PathVariable String titulo) {
        cursoService.deletarCurso(titulo);
        return ResponseEntity.noContent().build();
    }
}
