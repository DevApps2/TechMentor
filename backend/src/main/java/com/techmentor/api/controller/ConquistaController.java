package com.techmentor.api.controller;

import com.techmentor.api.dto.GamificacaoDtos.ConquistaRequest;
import com.techmentor.api.dto.GamificacaoDtos.ConquistaResponse;
import com.techmentor.api.dto.GamificacaoDtos.UsuarioConquistaResponse;
import com.techmentor.api.service.ConquistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conquistas")
@Tag(name = "Conquista", description = "Gerenciar as conquistas da gamificação")
public class ConquistaController {

    private final ConquistaService conquistaService;

    @Operation(summary = "Criar conquista")
    @PostMapping
    public ResponseEntity<ConquistaResponse> criarConquista(@Valid @RequestBody ConquistaRequest request) {
        return ResponseEntity.status(201).body(conquistaService.criarConquista(request));
    }

    @Operation(summary = "Listar conquistas")
    @GetMapping
    public ResponseEntity<List<ConquistaResponse>> listarConquistas() {
        return ResponseEntity.ok(conquistaService.listarConquistasDTO());
    }

    @Operation(summary = "Listar conquistas de uma usuária")
    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<UsuarioConquistaResponse>> listarConquistasDaUsuaria(
            @Parameter(description = "ID da usuária") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(conquistaService.listarConquistasDaUsuariaDTO(usuarioId));
    }

    @Operation(summary = "Atualizar conquista")
    @PutMapping("/{nome}")
    public ResponseEntity<ConquistaResponse> atualizarConquista(@Parameter(description = "Nome da conquista") @PathVariable String nome,
                                                                @Valid @RequestBody ConquistaRequest request) {
        return ResponseEntity.ok(conquistaService.atualizarConquista(nome, request));
    }

    @Operation(summary = "Deletar conquista")
    @DeleteMapping("/{nome}")
    public ResponseEntity<Void> deletarConquista(@Parameter(description = "Nome da conquista") @PathVariable String nome) {
        conquistaService.deletarConquista(nome);
        return ResponseEntity.noContent().build();
    }
}
