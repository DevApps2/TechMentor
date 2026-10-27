package com.techmentor.api.dto;

import com.techmentor.api.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contratos de entrada e saída dos endpoints de usuário. */
public final class UsuarioDtos {
    private UsuarioDtos() { }

    @Schema(description = "Dados para cadastrar um usuário")
    public record CreateRequest(
            @Schema(description = "Nome completo do usuário", example = "Ana Souza")
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            @Schema(description = "E-mail do usuário, usado no cadastro e deve ser único", example = "ana.souza@email.com")
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
            String email,

            @Schema(description = "Senha de acesso, entre 8 e 72 caracteres", example = "senhaSegura123", minLength = 8, maxLength = 72)
            @NotBlank(message = "A senha é obrigatória")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
            String senha
    ) { }

    @Schema(description = "Dados para atualizar um usuário")
    public record UpdateRequest(
            @Schema(description = "Nome completo do usuário", example = "Ana Souza")
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            @Schema(description = "E-mail do usuário", example = "ana.souza@email.com")
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
            String email,

            @Schema(description = "URL da foto de perfil", example = "https://cdn.techmentor.com/fotos/ana.png")
            @Size(max = 255, message = "A URL da foto deve ter no máximo 255 caracteres")
            String fotoUrl
    ) { }

    @Schema(description = "Dados públicos de um usuário")
    public record Response(
            @Schema(description = "ID do usuário", example = "1")
            Long id,
            @Schema(description = "Nome completo do usuário", example = "Ana Souza")
            String nome,
            @Schema(description = "E-mail do usuário", example = "ana.souza@email.com")
            String email,
            @Schema(description = "URL da foto de perfil", example = "https://cdn.techmentor.com/fotos/ana.png")
            String fotoUrl,
            @Schema(description = "Tipo do usuário", example = "ALUNA")
            Usuario.Tipo tipo,
            @Schema(description = "Total de XP acumulado", example = "250")
            int xpTotal,
            @Schema(description = "Nível atual do usuário", example = "3")
            int nivel,
            @Schema(description = "Dias consecutivos de atividade", example = "5")
            int sequenciaDias
    ) {
        public static Response from(Usuario usuario) {
            return new Response(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                    usuario.getFotoUrl(), usuario.getTipo(), usuario.getXpTotal(),
                    usuario.getNivel(), usuario.getSequenciaDias());
        }
    }
}
