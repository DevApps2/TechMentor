package com.techmentor.api.dto;

import com.techmentor.api.model.SituacaoPedido;
import com.techmentor.api.model.TipoParceira;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Contratos do pedido de parceria feito pelo site e da fila de análise da administradora. */
public final class PedidoParceriaDtos {
    private PedidoParceriaDtos() { }

    @Schema(description = "O que a instituição preenche no formulário \"Seja nosso parceiro\". Rota pública, sem login")
    public record PedidoRequest(
            @Schema(description = "Nome da instituição; é o crédito que as alunas veem nos cursos", example = "Instituto Aprender")
            @NotBlank(message = "O nome da instituição é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres") String nome,
            @Schema(description = "EMPRESA (exige CNPJ), ONG ou INDEPENDENTE", example = "ONG")
            @NotNull(message = "Informe o tipo") TipoParceira tipo,
            @Schema(description = "CNPJ, com ou sem pontuação; obrigatório para EMPRESA", example = "11.222.333/0001-81", nullable = true)
            @Size(max = 18, message = "CNPJ inválido") String documento,
            @Schema(description = "Site oficial, começando com http:// ou https://", example = "https://www.institutoaprender.org.br", nullable = true)
            @Size(max = 200, message = "O site deve ter no máximo 200 caracteres")
            @Pattern(regexp = "^$|^https?://\\S+$", message = "O site deve começar com http:// ou https://") String site,
            @Schema(description = "Nome de quem responde pela instituição", example = "Marina Duarte")
            @NotBlank(message = "O nome do responsável é obrigatório")
            @Size(max = 120, message = "O nome do responsável deve ter no máximo 120 caracteres") String responsavel,
            @Schema(description = "E-mail de contato; é para ele que vai o convite, se o pedido for aprovado", example = "marina@institutoaprender.org.br")
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres") String email,
            @Schema(description = "O que a instituição pretende oferecer (de 20 a 1000 caracteres)", example = "Temos 12 cursos de lógica e web em vídeo, gratuitos, no YouTube.")
            @NotBlank(message = "Conte o que você pretende oferecer")
            @Size(min = 20, max = 1000, message = "Conte um pouco mais: a mensagem deve ter entre 20 e 1000 caracteres") String mensagem,
            @Schema(description = "Precisa ser true: a pessoa leu e aceita a política de privacidade", example = "true")
            @NotNull(message = "É preciso aceitar a política de privacidade") Boolean aceitaPrivacidade,
            @Schema(description = "A versão da política de privacidade exibida no site; fica guardada como prova do consentimento", example = "2026-11-18")
            @NotBlank(message = "Informe a versão da política") @Size(max = 20, message = "Versão inválida") String versaoPolitica,
            @Schema(description = "ARMADILHA CONTRA ROBÔS: campo escondido no site que pessoas não preenchem. Deixe vazio ou omita.", nullable = true)
            @Size(max = 200) String website) { }

    @Schema(description = "Resposta do pedido. É sempre a mesma, para não revelar se o e-mail já existe")
    public record PedidoRecebidoResponse(String mensagem) { }

    @Schema(description = "Um pedido de parceria, como a administradora vê")
    public record PedidoResponse(
            Long id,
            String nome,
            TipoParceira tipo,
            @Schema(description = "Só os dígitos do CNPJ", nullable = true) String documento,
            @Schema(nullable = true) String site,
            String responsavel,
            String email,
            String mensagem,
            SituacaoPedido situacao,
            LocalDateTime criadoEm,
            @Schema(description = "O domínio do e-mail bate com o do site? Nulo se não há site. É só uma pista para a análise", nullable = true)
            Boolean dominioDoEmailConfereComSite,
            @Schema(description = "Quando aceitou a política de privacidade") LocalDateTime consentimentoEm,
            @Schema(description = "A versão da política que ela viu") String politicaVersao,
            @Schema(nullable = true) LocalDateTime decididoEm,
            @Schema(description = "Quem decidiu", nullable = true) String decididoPor,
            @Schema(description = "Motivo da recusa, quando RECUSADO", nullable = true) String motivoDaRecusa,
            @Schema(description = "A parceira criada, quando APROVADO", nullable = true) Long parceiraId) { }

    @Schema(description = "Por que o pedido está sendo recusado. A instituição lê este texto por e-mail")
    public record RecusaPedidoRequest(
            @Schema(example = "No momento só aceitamos cursos em português.")
            @NotBlank(message = "Informe o motivo")
            @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres") String motivo) { }
}
