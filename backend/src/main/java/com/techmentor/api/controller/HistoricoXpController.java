package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.HistoricoXpResponse;
import com.techmentor.api.service.XpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/historico-xp")
@Tag(name = "Histórico de XP", description = "Consultar o histórico de XP das usuárias")
public class HistoricoXpController {

    private final XpService xpService;

    @Operation(summary = "Listar histórico de XP de uma usuária")
    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<HistoricoXpResponse>> listar(@Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(xpService.listarDTO(usuarioId));
    }
}
