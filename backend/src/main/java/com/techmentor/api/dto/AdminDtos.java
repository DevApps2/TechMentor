package com.techmentor.api.dto;

import com.techmentor.api.model.Denuncia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Contratos dos endpoints que só administradoras podem usar. */
public final class AdminDtos {
    private AdminDtos() { }

    @Schema(description = "Dados para cadastrar uma mentora: a conta de acesso e o perfil de uma vez")
    public record MentoraCadastroRequest(
            @Schema(description = "Nome completo da mentora", example = "Maria Silva")
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres") String nome,
            @Schema(description = "E-mail de acesso, deve ser único", example = "maria.silva@techmentor.com")
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres") String email,
            @Schema(description = "Senha provisória, entre 8 e 72 caracteres", example = "senhaProvisoria1", minLength = 8, maxLength = 72)
            @NotBlank(message = "A senha é obrigatória")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
            @Schema(description = "Biografia da mentora", example = "Engenheira de software apaixonada por ensinar", nullable = true)
            @Size(max = 2000, message = "A biografia deve ter no máximo 2000 caracteres") String bio,
            @Schema(description = "Cargo atual", example = "Engenheira de Software Sênior", nullable = true)
            @Size(max = 255, message = "O cargo deve ter no máximo 255 caracteres") String cargo,
            @Schema(description = "Empresa atual", example = "Acme Tecnologia", nullable = true)
            @Size(max = 255, message = "A empresa deve ter no máximo 255 caracteres") String empresa,
            @Schema(description = "Anos de experiência profissional", example = "8", nullable = true)
            @PositiveOrZero(message = "Os anos de experiência não podem ser negativos") Integer anosExperiencia,
            @Schema(description = "IDs das áreas de atuação da mentora", example = "[1, 2]", nullable = true)
            Set<@Positive Long> areasIds) { }

    @Schema(description = "Novo status de uma denúncia")
    public record DenunciaStatusRequest(
            @Schema(description = "ABERTA, EM_ANALISE ou RESOLVIDA", example = "EM_ANALISE")
            @NotNull(message = "O status é obrigatório") Denuncia.Status status) { }
}
