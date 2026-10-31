package com.techmentor.api.dto;

import com.techmentor.api.model.ConteudoSeguranca;
import com.techmentor.api.model.Denuncia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class SegurancaDtos {
    private SegurancaDtos() { }

    @Schema(description = "Dados para registrar uma denúncia")
    public record DenunciaRequest(
            @Schema(description = "ID do usuário denunciado; opcional em denúncias gerais", example = "5", nullable = true)
            @Positive Long usuarioDenunciadoId,
            @Schema(description = "Motivo da denúncia", example = "ASSEDIO")
            @NotNull Denuncia.Motivo motivo,
            @Schema(description = "Detalhes da denúncia (mínimo de 10 caracteres)", example = "Mensagens ofensivas enviadas no chat", minLength = 10, maxLength = 2000)
            @NotBlank(message = "Descreva a situação")
            @Size(min = 10, max = 2000, message = "A descrição deve ter entre 10 e 2000 caracteres") String descricao) { }

    @Schema(description = "Denúncia registrada na plataforma")
    public record DenunciaResponse(
            @Schema(description = "ID da denúncia", example = "1")
            Long id,
            @Schema(description = "ID de quem denunciou", example = "2")
            Long denuncianteId,
            @Schema(description = "ID do usuário denunciado; nulo em denúncias gerais", example = "5", nullable = true)
            Long denunciadoId,
            @Schema(description = "Motivo da denúncia", example = "ASSEDIO")
            Denuncia.Motivo motivo,
            @Schema(description = "Detalhes da denúncia", example = "Mensagens ofensivas enviadas no chat")
            String descricao,
            @Schema(description = "Situação da denúncia", example = "ABERTA")
            Denuncia.Status status,
            @Schema(description = "Data e hora em que a denúncia foi criada", example = "2026-10-26T14:30:00")
            LocalDateTime criadaEm) { }

    @Schema(description = "Dados para bloquear um usuário")
    public record BloqueioRequest(
            @Schema(description = "ID do usuário a ser bloqueado", example = "5")
            @NotNull @Positive Long usuarioId) { }

    @Schema(description = "Orientação de segurança ou canal de ajuda")
    public record ConteudoSegurancaResponse(
            @Schema(description = "ID do conteúdo", example = "1")
            Long id,
            @Schema(description = "ORIENTACAO (dica de segurança) ou CANAL_AJUDA (telefone, e-mail ou link)", example = "CANAL_AJUDA")
            ConteudoSeguranca.Tipo tipo,
            @Schema(description = "Título", example = "Apoio emocional")
            String titulo,
            @Schema(description = "Texto do conteúdo", example = "O CVV atende gratuitamente pelo número 188.", nullable = true)
            String descricao,
            @Schema(description = "Contato do canal de ajuda; nulo nas orientações", example = "188", nullable = true)
            String contato) {
        public static ConteudoSegurancaResponse from(ConteudoSeguranca conteudo) {
            return new ConteudoSegurancaResponse(conteudo.getId(), conteudo.getTipo(), conteudo.getTitulo(),
                    conteudo.getDescricao(), conteudo.getContato());
        }
    }

    @Schema(description = "Usuário bloqueado")
    public record BloqueioResponse(
            @Schema(description = "ID do usuário bloqueado", example = "5")
            Long usuarioId,
            @Schema(description = "Nome do usuário bloqueado", example = "Carlos Lima")
            String nome,
            @Schema(description = "Data e hora do bloqueio", example = "2026-10-26T14:30:00")
            LocalDateTime criadoEm) { }
}
