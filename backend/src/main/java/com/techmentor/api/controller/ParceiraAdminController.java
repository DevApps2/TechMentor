package com.techmentor.api.controller;

import com.techmentor.api.dto.AuditoriaDtos.RegistroResponse;
import com.techmentor.api.dto.ParceiraDtos.ConviteRequest;
import com.techmentor.api.dto.ParceiraDtos.MotivoRequest;
import com.techmentor.api.dto.ParceiraDtos.ParceiraResponse;
import com.techmentor.api.dto.ParceiraDtos.VerificacaoRequest;
import com.techmentor.api.model.RegistroAuditoria;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.service.AuditoriaService;
import com.techmentor.api.service.ParceiraService;
import com.techmentor.api.service.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** A administradora convida, verifica, suspende e reativa parceiras. Tudo sob /api/admin (só ADMIN). */
@RestController
@RequestMapping("/api/admin/parceiras")
@RequiredArgsConstructor
@Tag(name = "Parceiras (administradora)", description = "Convidar empresas parceiras, verificar o cadastro e suspender ou reativar")
public class ParceiraAdminController {

    private final ParceiraService parceiras;
    private final AuditoriaService auditoria;
    private final UsuarioAtualService usuarios;

    @Operation(summary = "Listar parceiras", description = "Filtro opcional por situação (CONVIDADA, ATIVA, SUSPENSA).")
    @GetMapping
    public List<ParceiraResponse> listar(@RequestParam(required = false) SituacaoParceira situacao) {
        return parceiras.listar(usuarios.buscarAtual(), situacao);
    }

    @Operation(summary = "Detalhar parceira", description = "Inclui o checklist da verificação e se o domínio do e-mail bate com o do site.")
    @GetMapping("/{id}")
    public ParceiraResponse detalhar(@PathVariable Long id) {
        return parceiras.detalhar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Convidar parceira",
            description = "Cria a conta (inativa) e manda um código por e-mail. A parceira cria a própria senha e aceita os termos. "
                    + "409 se o e-mail, o nome ou o CNPJ já existem; 400 se o CNPJ é inválido ou falta numa empresa.")
    @PostMapping
    public ResponseEntity<ParceiraResponse> convidar(@Valid @RequestBody ConviteRequest request) {
        return ResponseEntity.status(201).body(parceiras.convidar(usuarios.buscarAtual(), request));
    }

    @Operation(summary = "Reenviar o convite", description = "Gera um código novo (o anterior deixa de valer). Só antes de a parceira aceitar; no máximo um por minuto.")
    @PostMapping("/{id}/convite")
    public ResponseEntity<Void> reenviar(@PathVariable Long id) {
        parceiras.reenviarConvite(usuarios.buscarAtual(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verificar parceira",
            description = "Registra o checklist (site, domínio do e-mail e identificação, os três conferidos) e libera a criação de cursos. Só depois de a parceira aceitar o convite.")
    @PutMapping("/{id}/verificacao")
    public ParceiraResponse verificar(@PathVariable Long id, @Valid @RequestBody VerificacaoRequest request) {
        return parceiras.verificar(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Suspender parceira", description = "Ela deixa de editar; o que já está publicado continua no ar até a administradora decidir. O motivo é enviado a ela.")
    @PostMapping("/{id}/suspensao")
    public ParceiraResponse suspender(@PathVariable Long id, @Valid @RequestBody MotivoRequest request) {
        return parceiras.suspender(usuarios.buscarAtual(), id, request.motivo());
    }

    @Operation(summary = "Reativar parceira")
    @PostMapping("/{id}/reativacao")
    public ParceiraResponse reativar(@PathVariable Long id) {
        return parceiras.reativar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Histórico da parceira", description = "Convite, aceite dos termos, verificação, suspensões.")
    @GetMapping("/{id}/historico")
    public List<RegistroResponse> historico(@PathVariable Long id) {
        parceiras.detalhar(usuarios.buscarAtual(), id);   // 404 se não existe
        return auditoria.historico(RegistroAuditoria.PARCEIRA, id);
    }
}
