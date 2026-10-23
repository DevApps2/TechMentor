package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.NotificacaoResponse;
import com.techmentor.api.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<NotificacaoResponse>> listar(@PathVariable Long usuarioId,
                                                            @RequestParam(required = false) Boolean lida) {
        return ResponseEntity.ok(notificacaoService.listarDTO(usuarioId, lida));
    }

    @PatchMapping("/leitura/usuarios/{usuarioId}")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long usuarioId,
                                               @RequestParam(required = false) Long id) {
        notificacaoService.marcarComoLida(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
