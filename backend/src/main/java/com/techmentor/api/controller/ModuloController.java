package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.ModuloRequest;
import com.techmentor.api.dto.CursoDtos.ModuloDeleteRequest;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.techmentor.api.service.ModuloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/modulos")
@Tag(name = "Módulo", description = "Gerenciar os módulos de um curso")
public class ModuloController {

    private final ModuloService moduloService;

    @Operation(summary = "Criar módulo em um curso")
    @PostMapping("/{titulo}")
    public ResponseEntity<ModuloResponse> criarModulo(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                      @Valid @RequestBody ModuloRequest request) {
        return ResponseEntity.status(201).body(moduloService.criarModulo(titulo, request));
    }

    @Operation(summary = "Deletar módulo (legado)",
            description = "Recebe a ordem do módulo no corpo da requisição. Prefira DELETE /modulos/{titulo}/{ordem}.",
            deprecated = true)
    @DeleteMapping("/{titulo}")
    public ResponseEntity<Void> deletarModuloLegado(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                    @Valid @RequestBody ModuloDeleteRequest request) {
        moduloService.deletarModulo(titulo, request.ordem());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deletar módulo")
    @DeleteMapping("/{titulo}/{ordem}")
    public ResponseEntity<Void> deletarModulo(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                              @Parameter(description = "Ordem do módulo no curso") @PathVariable int ordem) {
        moduloService.deletarModulo(titulo, ordem);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Atualizar módulo")
    @PutMapping("/{titulo}/{ordem}")
    public ResponseEntity<ModuloResponse> atualizarModulo(@Parameter(description = "Título do curso") @PathVariable String titulo,
                                                          @Parameter(description = "Ordem do módulo no curso") @PathVariable int ordem,
                                                          @Valid @RequestBody ModuloRequest request) {
        return ResponseEntity.ok(moduloService.atualizarModulo(titulo, ordem, request));
    }
}
