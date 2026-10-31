package com.techmentor.api.controller;

import com.techmentor.api.dto.SenhaDtos.EsqueciRequest;
import com.techmentor.api.dto.SenhaDtos.RedefinirRequest;
import com.techmentor.api.service.RecuperacaoSenhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/usuarios/senha")
@Tag(name = "Recuperação de senha", description = "Pedir um código por e-mail e redefinir a senha")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService service;

    @Operation(summary = "Pedir código de recuperação",
            description = "Envia um código de 6 dígitos por e-mail, válido por 10 minutos. "
                    + "A resposta é sempre a mesma, exista ou não o e-mail, para não revelar quem tem conta.")
    @PostMapping("/esqueci")
    public ResponseEntity<Void> esqueci(@Valid @RequestBody EsqueciRequest dto) {
        service.solicitarCodigo(dto.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redefinir a senha",
            description = "Confere o código recebido por e-mail e troca a senha. "
                    + "Retorna 400 se o código estiver errado, expirado ou com tentativas esgotadas.")
    @PostMapping("/redefinir")
    public ResponseEntity<Void> redefinir(@Valid @RequestBody RedefinirRequest dto) {
        service.redefinirSenha(dto.email(), dto.codigo(), dto.novaSenha());
        return ResponseEntity.noContent().build();
    }
}