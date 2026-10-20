package com.techmentor.api.controller;

import com.techmentor.api.model.ProgressoAula;
import com.techmentor.api.service.ProgressoAulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/progresso")
public class ProgressoAulaController {

    private final ProgressoAulaService progressoAulaService;

    @PostMapping("/{titulo}/{ordemModulo}/{ordemAula}/usuarios/{usuarioId}")
    public ResponseEntity<ProgressoAula> concluirAula(@PathVariable String titulo, @PathVariable int ordemModulo, @PathVariable int ordemAula, @PathVariable Long usuarioId) {
            this.progressoAulaService.concluirAula(titulo, ordemModulo, ordemAula, usuarioId);
            return ResponseEntity.status(201).build();
    }

    @GetMapping("/{titulo}/usuarios/{usuarioId}")
    public ResponseEntity<Map<String, Long>> progressoDoCurso(@PathVariable String titulo,
                                                              @PathVariable Long usuarioId) {
        return ResponseEntity.ok(progressoAulaService.progressoDoCurso(titulo, usuarioId));
    }
}
