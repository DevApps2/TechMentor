package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.ProgressoAulaResponse;
import com.techmentor.api.dto.GamificacaoDtos.ProgressoCursoResponse;
import com.techmentor.api.service.ProgressoAulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/progresso")
public class ProgressoAulaController {

    private final ProgressoAulaService progressoAulaService;

    @PostMapping("/{titulo}/{ordemModulo}/{ordemAula}/usuarios/{usuarioId}")
    public ResponseEntity<ProgressoAulaResponse> concluirAula(@PathVariable String titulo,
                                                              @PathVariable int ordemModulo,
                                                              @PathVariable int ordemAula,
                                                              @PathVariable Long usuarioId) {
        return ResponseEntity.status(201)
                .body(progressoAulaService.concluirAulaDTO(titulo, ordemModulo, ordemAula, usuarioId));
    }

    @GetMapping("/{titulo}/usuarios/{usuarioId}")
    public ResponseEntity<ProgressoCursoResponse> progressoDoCurso(@PathVariable String titulo,
                                                                  @PathVariable Long usuarioId) {
        return ResponseEntity.ok(progressoAulaService.progressoDoCursoDTO(titulo, usuarioId));
    }
}
