package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.AulaRequest;
import com.techmentor.api.dto.CursoDtos.AulaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.techmentor.api.service.AulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/aulas")
@Tag(name = "Aula", description = "Gerenciar as aulas dos módulos de um curso")
public class AulaController {
    private final AulaService aulaService;

    @Operation(summary = "Criar aula em um módulo do curso")
    @PostMapping("/{titulo}/{ordemModulo}")
    public ResponseEntity<AulaResponse> criarAula(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                  @Parameter(description = "Ordem do módulo no curso") @PathVariable int ordemModulo,
                                                  @Valid @RequestBody AulaRequest request) {
        return ResponseEntity.status(201).body(aulaService.criarAula(titulo, ordemModulo, request));
    }

    @Operation(summary = "Deletar aula")
    @DeleteMapping("/{titulo}/{ordemModulo}/{ordemAula}")
    public ResponseEntity<Void> deletarAula(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                            @Parameter(description = "Ordem do módulo no curso") @PathVariable int ordemModulo,
                                            @Parameter(description = "Ordem da aula no módulo") @PathVariable int ordemAula) {
        aulaService.deletarAula(titulo, ordemModulo, ordemAula);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Atualizar aula")
    @PutMapping("/{titulo}/{ordemModulo}/{ordemAula}")
    public ResponseEntity<AulaResponse> atualizarAula(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                      @Parameter(description = "Ordem do módulo no curso") @PathVariable int ordemModulo,
                                                      @Parameter(description = "Ordem da aula no módulo") @PathVariable int ordemAula,
                                                      @Valid @RequestBody AulaRequest request) {
        return ResponseEntity.ok(aulaService.atualizarAula(titulo, ordemModulo, ordemAula, request));
    }
}
