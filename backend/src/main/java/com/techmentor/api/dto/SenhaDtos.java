package com.techmentor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contratos de entrada dos endpoints de recuperação de senha. */
public final class SenhaDtos {
    private SenhaDtos() { }

    @Schema(description = "Pedido de código para redefinir a senha")
    public record EsqueciRequest(
            @Schema(description = "E-mail da conta", example = "ana.souza@email.com")
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            String email
    ) { }

    @Schema(description = "Código recebido por e-mail e a nova senha")
    public record RedefinirRequest(
            @Schema(description = "E-mail da conta", example = "ana.souza@email.com")
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            String email,

            @Schema(description = "Código de 6 dígitos recebido por e-mail", example = "482915")
            @NotBlank(message = "O código é obrigatório")
            @Pattern(regexp = "\\d{6}", message = "O código deve ter 6 dígitos")
            String codigo,

            @Schema(description = "Nova senha, entre 8 e 72 caracteres", example = "novaSenha123")
            @NotBlank(message = "A senha é obrigatória")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
            String novaSenha
    ) { }
}