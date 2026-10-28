package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.AulaProgressoResponse;
import com.techmentor.api.dto.GamificacaoDtos.ProgressoAulaResponse;
import com.techmentor.api.dto.GamificacaoDtos.ProgressoCursoResponse;
import com.techmentor.api.service.ProgressoAulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/progresso")
@Tag(name = "Progresso", description = "Concluir aulas e acompanhar o progresso nos cursos")
public class ProgressoAulaController {

    private final ProgressoAulaService progressoAulaService;

    @Operation(summary = "Listar aulas do curso com o estado de cada uma",
            description = "Cada aula vem como CONCLUIDA, ATUAL (a próxima a estudar) ou BLOQUEADA, na ordem de estudo.")
    @GetMapping("/{titulo}/usuarios/{usuarioId}/aulas")
    public ResponseEntity<List<AulaProgressoResponse>> listarAulas(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                                   @Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(progressoAulaService.listarAulasDoCurso(titulo, usuarioId));
    }

    @Operation(summary = "Concluir aula",
            description = "Conclui a aula (a anterior precisa estar concluída) e matricula a usuária no curso se ainda não estiver.")
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
