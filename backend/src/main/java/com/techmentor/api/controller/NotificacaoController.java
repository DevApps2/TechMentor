package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.NotificacaoResponse;
import com.techmentor.api.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notificacoes")
@Tag(name = "Notificação", description = "Listar notificações e marcá-las como lidas")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @Operation(summary = "Listar notificações de uma usuária")
    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<NotificacaoResponse>> listar(@Parameter(description = "ID da usuária") @PathVariable Long usuarioId,
                                                            @Parameter(description = "Filtra por lidas (true) ou não lidas (false); omita para todas")
                                                            @RequestParam(required = false) Boolean lida) {
        return ResponseEntity.ok(notificacaoService.listarDTO(usuarioId, lida));
    }

    @Operation(summary = "Marcar notificações como lidas",
            description = "Marca uma notificação específica como lida; sem o parâmetro id, marca todas da usuária.")
    @PatchMapping("/leitura/usuarios/{usuarioId}")
    public ResponseEntity<Void> marcarComoLida(@Parameter(description = "ID da usuária") @PathVariable Long usuarioId,
                                               @Parameter(description = "ID da notificação (opcional)") @RequestParam(required = false) Long id) {
        notificacaoService.marcarComoLida(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
