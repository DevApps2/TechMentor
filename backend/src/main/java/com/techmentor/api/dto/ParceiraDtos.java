package com.techmentor.api.dto;

import com.techmentor.api.model.Parceira;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.TipoParceira;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Contratos da parceria: convite, aceite, verificação e perfil. */
public final class ParceiraDtos {
    private ParceiraDtos() { }

    @Schema(description = "Dados para convidar uma parceira. A administradora não define senha: a parceira cria a dela com o código enviado por e-mail")
    public record ConviteRequest(
            @Schema(description = "Nome da instituição; é o crédito que as alunas veem no curso", example = "Curso em Vídeo")
            @NotBlank(message = "O nome da instituição é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres") String nome,
            @Schema(description = "EMPRESA (exige CNPJ), ONG ou INDEPENDENTE", example = "ONG")
            @NotNull(message = "Informe o tipo da parceira") TipoParceira tipo,
            @Schema(description = "CNPJ, com ou sem pontuação; obrigatório para EMPRESA", example = "11.222.333/0001-81", nullable = true)
            @Size(max = 18, message = "CNPJ inválido") String documento,
            @Schema(description = "Site oficial, começando com http:// ou https://", example = "https://www.cursoemvideo.com", nullable = true)
            @Size(max = 200, message = "O site deve ter no máximo 200 caracteres")
            @Pattern(regexp = "^$|^https?://\\S+$", message = "O site deve começar com http:// ou https://") String site,
            @Schema(description = "Nome de quem responde pela parceira", example = "Gustavo Guanabara")
            @NotBlank(message = "O nome do responsável é obrigatório")
            @Size(max = 120, message = "O nome do responsável deve ter no máximo 120 caracteres") String responsavel,
            @Schema(description = "E-mail de acesso, que recebe o convite", example = "contato@cursoemvideo.com")
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Informe um e-mail válido")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres") String email) { }

    @Schema(description = "A parceira aceita o convite: cria a senha e aceita os Termos de Parceria")
    public record AceiteRequest(
            @Schema(description = "O e-mail que recebeu o convite", example = "contato@cursoemvideo.com")
            @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Informe um e-mail válido") String email,
            @Schema(description = "Código do convite, como veio no e-mail (com ou sem o hífen)", example = "K7M2-9QXA")
            @NotBlank(message = "O código é obrigatório") @Size(max = 20, message = "Código inválido") String codigo,
            @Schema(description = "Senha nova, entre 8 e 72 caracteres", minLength = 8, maxLength = 72)
            @NotBlank(message = "A senha é obrigatória")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
            @Schema(description = "Precisa ser true: a parceira leu e aceita os termos", example = "true")
            @NotNull(message = "É preciso aceitar os termos") Boolean aceitaTermos,
            @Schema(description = "A versão dos termos que ela viu (GET /parceiras/termos); se mudou, o aceite é recusado", example = "2026-11-14")
            @NotBlank(message = "Informe a versão dos termos") String versaoTermos) { }

    @Schema(description = "O resultado da verificação feita pela administradora. Os três itens precisam estar conferidos")
    public record VerificacaoRequest(
            @Schema(description = "O site existe e é da instituição") boolean siteConferido,
            @Schema(description = "O e-mail é do mesmo domínio do site (e não um e-mail pessoal)") boolean dominioDoEmailConferido,
            @Schema(description = "O CNPJ (ou o documento do responsável) foi conferido") boolean identificacaoConferida,
            @Schema(description = "Anotação livre da administradora", nullable = true)
            @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres") String observacao) { }

    @Schema(description = "Por que a parceira está sendo suspensa")
    public record MotivoRequest(
            @Schema(description = "Motivo; a parceira lê este texto", example = "Vídeos removidos do YouTube")
            @NotBlank(message = "Informe o motivo")
            @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres") String motivo) { }

    @Schema(description = "O que foi conferido na verificação")
    public record ChecklistResponse(
            boolean siteConferido,
            boolean dominioDoEmailConferido,
            boolean identificacaoConferida,
            @Schema(nullable = true) LocalDateTime verificadaEm,
            @Schema(nullable = true) String verificadaPor,
            @Schema(nullable = true) String observacao) { }

    @Schema(description = "Uma parceira")
    public record ParceiraResponse(
            Long id,
            @Schema(description = "ID da conta de acesso (Usuario)") Long usuarioId,
            String nome,
            TipoParceira tipo,
            @Schema(description = "Só os dígitos do CNPJ", nullable = true) String documento,
            @Schema(nullable = true) String site,
            String responsavel,
            String email,
            SituacaoParceira situacao,
            LocalDateTime criadaEm,
            @Schema(description = "A parceira já criou a senha e aceitou os termos?") boolean aceitouOsTermos,
            @Schema(nullable = true) String termosVersao,
            @Schema(nullable = true) LocalDateTime termosAceitosEm,
            @Schema(description = "O domínio do e-mail bate com o do site? Nulo se não há site. É só uma pista para a verificação", nullable = true)
            Boolean dominioDoEmailConfereComSite,
            ChecklistResponse verificacao,
            @Schema(description = "Motivo da suspensão, quando SUSPENSA", nullable = true) String motivoDaSuspensao,
            @Schema(description = "Quantos cursos ela tem, em qualquer situação") long cursos) { }

    @Schema(description = "Os Termos de Parceria vigentes")
    public record TermosResponse(
            @Schema(description = "Mande esta versão no aceite", example = "2026-11-14") String versao,
            String titulo,
            String texto) { }

    public static ChecklistResponse checklist(Parceira p) {
        return new ChecklistResponse(p.isSiteConferido(), p.isDominioDoEmailConferido(), p.isIdentificacaoConferida(),
                p.getVerificadaEm(), p.getVerificadaPor() == null ? null : p.getVerificadaPor().getNome(),
                p.getObservacaoDaVerificacao());
    }
}
