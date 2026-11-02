package com.techmentor.api.controller;

import com.techmentor.api.dto.AdminDtos.DenunciaStatusRequest;
import com.techmentor.api.dto.AdminDtos.MentoraCadastroRequest;
import com.techmentor.api.dto.MentoriaDtos.MentoraResponse;
import com.techmentor.api.dto.SegurancaDtos.DenunciaResponse;
import com.techmentor.api.service.AdminService;
import com.techmentor.api.service.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administração", description = "Ações exclusivas de administradoras (envie o ID da administradora em X-Usuario-Id)")
public class AdminController {
    private final AdminService admin;
    private final UsuarioAtualService usuarios;

    public AdminController(AdminService admin, UsuarioAtualService usuarios) {
        this.admin = admin;
        this.usuarios = usuarios;
    }

    @Operation(summary = "Cadastrar mentora",
            description = "Cria a conta (tipo MENTORA) e o perfil de mentora de uma vez. Retorna 409 se o e-mail já existir.")
    @PostMapping("/mentoras")
    @ResponseStatus(HttpStatus.CREATED)
    public MentoraResponse cadastrarMentora(@RequestHeader("X-Usuario-Id") Long actorId,
                                            @Valid @RequestBody MentoraCadastroRequest request) {
        return admin.cadastrarMentora(usuarios.buscar(actorId), request);
    }

    @Operation(summary = "Alterar o status de uma denúncia", description = "ABERTA, EM_ANALISE ou RESOLVIDA.")
    @PatchMapping("/denuncias/{id}/status")
    public DenunciaResponse alterarStatusDenuncia(@RequestHeader("X-Usuario-Id") Long actorId,
                                                  @Parameter(description = "ID da denúncia") @PathVariable Long id,
                                                  @Valid @RequestBody DenunciaStatusRequest request) {
        return admin.alterarStatusDenuncia(usuarios.buscar(actorId), id, request.status());
    }
}
