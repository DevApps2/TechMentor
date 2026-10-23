package com.techmentor.api.dto;

import com.techmentor.api.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contratos de entrada e saída dos endpoints de usuário. */
public final class UsuarioDtos {
    private UsuarioDtos() { }

    public record CreateRequest(
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
            String email,

            @NotBlank(message = "A senha é obrigatória")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
            String senha
    ) { }

    public record UpdateRequest(
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
            String email,

            @Size(max = 255, message = "A URL da foto deve ter no máximo 255 caracteres")
            String fotoUrl
    ) { }

    public record Response(
            Long id,
            String nome,
            String email,
            String fotoUrl,
            Usuario.Tipo tipo,
            int xpTotal,
            int nivel,
            int sequenciaDias
    ) {
        public static Response from(Usuario usuario) {
            return new Response(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                    usuario.getFotoUrl(), usuario.getTipo(), usuario.getXpTotal(),
                    usuario.getNivel(), usuario.getSequenciaDias());
        }
    }
}
