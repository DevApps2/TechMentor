package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.HistoricoXpResponse;
import com.techmentor.api.service.XpService;
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
public class HistoricoXpController {

    private final XpService xpService;

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<HistoricoXpResponse>> listar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(xpService.listarDTO(usuarioId));
    }
}
