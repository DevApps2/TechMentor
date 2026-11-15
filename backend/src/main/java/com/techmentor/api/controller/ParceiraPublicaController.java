package com.techmentor.api.controller;

import com.techmentor.api.dto.ParceiraDtos.AceiteRequest;
import com.techmentor.api.dto.ParceiraDtos.ParceiraResponse;
import com.techmentor.api.dto.ParceiraDtos.TermosResponse;
import com.techmentor.api.service.ParceiraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** O que a parceira usa antes de ter conta ativa: ler os termos e aceitar o convite. Rotas públicas (sem token). */
@RestController
@RequestMapping("/parceiras")
@RequiredArgsConstructor
@Tag(name = "Convite de parceira", description = "Ler os Termos de Parceria e aceitar o convite recebido por e-mail")
public class ParceiraPublicaController {

    private final ParceiraService parceiras;

    @Operation(summary = "Termos de Parceria vigentes", description = "O app mostra o texto e manda de volta a versão no aceite.")
    @GetMapping("/termos")
    public TermosResponse termos() {
        return parceiras.termos();
    }

    @Operation(summary = "Aceitar o convite",
            description = "Confere o código do e-mail, cria a senha e registra o aceite dos termos (versão e data). "
                    + "Depois disso a parceira já entra no app, mas só cria cursos depois da verificação da administradora. "
                    + "400 se o código está errado, vencido ou com tentativas esgotadas, ou se a versão dos termos mudou.")
    @PostMapping("/convite/aceitar")
    public ParceiraResponse aceitar(@Valid @RequestBody AceiteRequest request) {
        return parceiras.aceitarConvite(request);
    }
}
