package com.techmentor.api.controller;

import com.techmentor.api.dto.PedidoParceriaDtos.PedidoResponse;
import com.techmentor.api.dto.PedidoParceriaDtos.RecusaPedidoRequest;
import com.techmentor.api.model.SituacaoPedido;
import com.techmentor.api.service.PedidoParceriaService;
import com.techmentor.api.service.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** A fila de pedidos de parceria vindos do site. Tudo sob /api/admin (só ADMIN). */
@RestController
@RequestMapping("/api/admin/pedidos-parceria")
@RequiredArgsConstructor
@Tag(name = "Pedidos de parceria (administradora)", description = "Analisar os pedidos feitos pelo site \"Seja nosso parceiro\": aprovar ou recusar")
public class PedidoParceriaAdminController {

    private final PedidoParceriaService pedidos;
    private final UsuarioAtualService usuarios;

    @Operation(summary = "Listar pedidos", description = "Do mais novo para o mais antigo; filtro opcional por situação (PENDENTE, APROVADO, RECUSADO).")
    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) SituacaoPedido situacao) {
        return pedidos.listar(usuarios.buscarAtual(), situacao);
    }

    @Operation(summary = "Detalhar pedido", description = "Inclui a pista de se o domínio do e-mail bate com o do site e a prova do consentimento.")
    @GetMapping("/{id}")
    public PedidoResponse detalhar(@PathVariable Long id) {
        return pedidos.detalhar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Aprovar pedido",
            description = "Cria a conta de parceira e envia o convite por e-mail (código e link). 409 se o e-mail, o nome ou o CNPJ já existem, ou se o pedido já foi respondido.")
    @PostMapping("/{id}/aprovacao")
    public PedidoResponse aprovar(@PathVariable Long id) {
        return pedidos.aprovar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Recusar pedido", description = "O motivo é obrigatório e vai por e-mail para a instituição.")
    @PostMapping("/{id}/recusa")
    public PedidoResponse recusar(@PathVariable Long id, @Valid @RequestBody RecusaPedidoRequest request) {
        return pedidos.recusar(usuarios.buscarAtual(), id, request.motivo());
    }

    @Operation(summary = "Apagar um pedido",
            description = "Apaga os dados do pedido (pedido de quem quer ser esquecida, de teste ou de spam). Só pedido pendente ou recusado: o aprovado já virou conta de parceira (409).")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        pedidos.apagar(usuarios.buscarAtual(), id);
        return ResponseEntity.noContent().build();
    }
}
