package com.techmentor.api.controller;

import com.techmentor.api.dto.ParceiraDtos.AceiteRequest;
import com.techmentor.api.dto.ParceiraDtos.ParceiraResponse;
import com.techmentor.api.dto.ParceiraDtos.TermosResponse;
import com.techmentor.api.dto.PedidoParceriaDtos.PedidoRecebidoResponse;
import com.techmentor.api.dto.PedidoParceriaDtos.PedidoRequest;
import com.techmentor.api.service.LimiteDePedidos;
import com.techmentor.api.service.ParceiraService;
import com.techmentor.api.service.PedidoParceriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** O que a instituição usa antes de ter conta: pedir a parceria, ler os termos e aceitar o convite. Rotas públicas (sem token). */
@RestController
@RequestMapping("/parceiras")
@RequiredArgsConstructor
@Tag(name = "Convite de parceira", description = "Pedir a parceria pelo site, ler os Termos de Parceria e aceitar o convite recebido por e-mail")
public class ParceiraPublicaController {

    private final ParceiraService parceiras;
    private final PedidoParceriaService pedidos;
    private final LimiteDePedidos limiteDePedidos;

    // true quando a API está atrás de um proxy (ngrok, nginx) e o IP real vem no X-Forwarded-For
    @Value("${app.proxy.confiavel:true}")
    private boolean proxyConfiavel;

    @Operation(summary = "Pedir a parceria (formulário \"Seja nosso parceiro\")",
            description = "Rota pública. Guarda o pedido, manda a confirmação por e-mail para a instituição e avisa a equipe. "
                    + "A resposta é sempre a mesma quando os dados são válidos, exista ou não o e-mail no sistema. "
                    + "400 para dados inválidos (CNPJ, campos, política não aceita); 429 se o mesmo endereço mandou pedidos demais em uma hora.")
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoRecebidoResponse> pedir(@Valid @RequestBody PedidoRequest request, HttpServletRequest http) {
        limiteDePedidos.registrar(ipDoCliente(http));
        pedidos.registrar(request);
        return ResponseEntity.status(201).body(new PedidoRecebidoResponse(
                "Recebemos o seu pedido. Nossa equipe vai analisar e responder por e-mail."));
    }

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

    /** O endereço de quem chamou. Só confia no X-Forwarded-For se a API estiver mesmo atrás de um proxy. */
    private String ipDoCliente(HttpServletRequest request) {
        if (proxyConfiavel) {
            String encaminhado = request.getHeader("X-Forwarded-For");
            if (encaminhado != null && !encaminhado.isBlank()) {
                return encaminhado.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
