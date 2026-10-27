package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.ProgressoAulaResponse;
import com.techmentor.api.dto.GamificacaoDtos.ProgressoCursoResponse;
import com.techmentor.api.service.ProgressoAulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/progresso")
@Tag(name = "Progresso", description = "Concluir aulas e acompanhar o progresso nos cursos")
public class ProgressoAulaController {

    private final ProgressoAulaService progressoAulaService;

    @Operation(summary = "Concluir aula")
    @PostMapping("/{titulo}/{ordemModulo}/{ordemAula}/usuarios/{usuarioId}")
    public ResponseEntity<ProgressoAulaResponse> concluirAula(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                              @Parameter(description = "Ordem do módulo no curso") @PathVariable int ordemModulo,
                                                              @Parameter(description = "Ordem da aula no módulo") @PathVariable int ordemAula,
                                                              @Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.status(201)
                .body(progressoAulaService.concluirAulaDTO(titulo, ordemModulo, ordemAula, usuarioId));
    }

    @Operation(summary = "Consultar progresso de uma usuária em um curso")
    @GetMapping("/{titulo}/usuarios/{usuarioId}")
    public ResponseEntity<ProgressoCursoResponse> progressoDoCurso(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                                  @Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(progressoAulaService.progressoDoCursoDTO(titulo, usuarioId));
    }
}
