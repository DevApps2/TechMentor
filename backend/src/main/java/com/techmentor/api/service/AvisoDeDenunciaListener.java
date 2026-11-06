package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

/**
 * Manda um e-mail para a equipe quando chega uma denúncia nova.
 *
 * - Só roda depois que a denúncia foi gravada de verdade (AFTER_COMMIT): se der erro ao salvar, ninguém é avisado.
 * - Roda em outra thread (@Async): o app não espera o SMTP para responder.
 * - Se o e-mail falhar, a denúncia continua salva; só fica um aviso no log (sem o texto do relato).
 */
@Component
public class AvisoDeDenunciaListener {

    private static final Logger log = LoggerFactory.getLogger(AvisoDeDenunciaListener.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmailService emailService;

    // Para onde vai o aviso. Vazio = não envia nada.
    @Value("${app.moderacao.email:}")
    private String destino;

    public AvisoDeDenunciaListener(EmailService emailService) {
        this.emailService = emailService;
    }

    private ModeloDeEmail montarModelo(DenunciaCriadaEvent e) {

        String denunciado = e.denunciadoId() == null
                ? "Denúncia geral (nenhuma pessoa indicada)"
                : e.denunciadoNome() + " (id " + e.denunciadoId() + ")";

        return ModeloDeEmail.com("Nova denúncia #" + e.id())
                .paragrafo("Uma nova denúncia foi registrada no TechMentor.")
                .detalhe("Número", "#" + e.id())
                .detalhe("Data", e.criadaEm().format(DATA))
                .detalhe("Motivo", AvisoDeDenunciaListener.rotuloDoMotivo(e.motivo()))
                .detalhe("Denunciante", e.denuncianteNome() + " (id " + e.denuncianteId() + ")")
                .detalhe("Denunciado", denunciado)
                .paragrafo("Relato:\n" + e.descricao())
                .paragrafo("Para atualizar a situação (EM_ANALISE ou RESOLVIDA), use no Swagger: PATCH /api/denuncias/"
                        + e.id() + "/status, autenticado como administrador (botão Authorize, com o token do login).")
                .nota("Este e-mail contém um relato sensível. Não encaminhe.");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void avisarEquipe(DenunciaCriadaEvent evento) {

        if (destino == null || destino.isBlank()) {
            return;
        }

        try {
            emailService.enviar(
                    destino,
                    "[TechMentor] Nova denúncia #" + evento.id() + " - " + rotuloDoMotivo(evento.motivo()),
                    montarModelo(evento)
            );
        } catch (Exception e) {
            log.warn("A denúncia #{} foi salva, mas não foi possível avisar a equipe por e-mail.", evento.id());
        }
    }

    private String montarTexto(DenunciaCriadaEvent e) {

        String denunciado = e.denunciadoId() == null
                ? "Denúncia geral (nenhuma pessoa indicada)"
                : e.denunciadoNome() + " (id " + e.denunciadoId() + ")";

        return "Nova denúncia registrada no TechMentor.\n\n"
                + "Número: #" + e.id() + "\n"
                + "Data: " + e.criadaEm().format(DATA) + "\n"
                + "Motivo: " + rotuloDoMotivo(e.motivo()) + "\n"
                + "Quem denunciou: " + e.denuncianteNome() + " (id " + e.denuncianteId() + ")\n"
                + "Quem foi denunciado: " + denunciado + "\n\n"
                + "Relato:\n" + e.descricao() + "\n\n"
                + "Para atualizar a situação (EM_ANALISE ou RESOLVIDA), use no Swagger:\n"
                + "PATCH /api/denuncias/" + e.id() + "/status, autenticado como administrador (botão Authorize, com o token do login).\n\n"
                + "Este e-mail contém um relato sensível. Não encaminhe.";
    }

    private static String rotuloDoMotivo(String motivo) {
        return switch (motivo) {
            case "ASSEDIO" -> "Assédio";
            case "COMPORTAMENTO_INADEQUADO" -> "Comportamento inadequado";
            case "SPAM" -> "Spam ou fraude";
            case "DISCURSO_DE_ODIO" -> "Discurso de ódio";
            case "PERFIL_FALSO" -> "Perfil falso";
            default -> "Outro";
        };
    }
}


