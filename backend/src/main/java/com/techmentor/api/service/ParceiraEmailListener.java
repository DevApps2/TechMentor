package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.CursoEnviadoParaRevisao;
import com.techmentor.api.evento.EventosDeNotificacao.CursoRevisado;
import com.techmentor.api.evento.EventosDeNotificacao.DecisaoDeRevisao;
import com.techmentor.api.evento.EventosDeNotificacao.ParceiraAceitouConvite;
import com.techmentor.api.evento.EventosDeNotificacao.SituacaoDaParceiraMudou;
import com.techmentor.api.model.SituacaoParceira;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Avisos de parceria e revisão pelo serviço de e-mail já usado pelo restante do aplicativo. */
@Component
public class ParceiraEmailListener {

    private static final Logger log = LoggerFactory.getLogger(ParceiraEmailListener.class);

    private final EmailService emailService;

    @Value("${app.moderacao.email:}")
    private String emailDaEquipe;

    public ParceiraEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoAceitarConvite(ParceiraAceitouConvite evento) {
        if (evento.email() == null || evento.email().isBlank()) return;
        ModeloDeEmail modelo = ModeloDeEmail.com("Cadastro recebido")
                .saudacao("Olá!")
                .paragrafo("Os Termos de Parceria foram aceitos para " + evento.nomeDaInstituicao() + ".")
                .paragrafo("A conta ficará em verificação até a equipe concluir a conferência. Você receberá outro e-mail quando o acesso para cadastrar cursos for ativado.");
        enviar("parceira-aceite:" + evento.eventoId(), evento.email(), "Cadastro recebido - TechMentor", modelo, true);
    }

    /** Ativar, suspender e reativar são mudanças de situação relevantes para a conta da parceira. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoMudarSituacaoDaParceira(SituacaoDaParceiraMudou evento) {
        String titulo = switch (evento.nova()) {
            case ATIVA -> "Sua parceria foi ativada";
            case SUSPENSA -> "Atualização da sua conta de parceira";
            case CONVIDADA -> "Atualização da sua conta de parceira";
        };
        ModeloDeEmail modelo = ModeloDeEmail.com(titulo)
                .saudacao("Olá, " + evento.parceira().getNome() + "!");

        if (evento.nova() == SituacaoParceira.ATIVA) {
            modelo.paragrafo("A verificação da sua instituição foi concluída. Sua conta está ativa e você já pode cadastrar cursos e enviá-los para revisão.");
        } else if (evento.nova() == SituacaoParceira.SUSPENSA) {
            modelo.paragrafo("Sua conta de parceira está suspensa no momento.")
                    .destaque("Motivo informado pela equipe", evento.motivo() == null ? "Entre em contato com a equipe TechMentor." : evento.motivo())
                    .nota("O conteúdo já publicado permanece disponível enquanto a equipe analisa a situação.");
        } else {
            modelo.paragrafo("A situação da sua conta foi atualizada. Entre no aplicativo para conferir os próximos passos.");
        }

        enviar("parceira-situacao:" + evento.eventoId(), evento.parceira().getEmail(), titulo, modelo, true);
    }

    /** A fila de revisão usa o endereço administrativo já configurado para notificações da equipe. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoEnviarCursoParaRevisao(CursoEnviadoParaRevisao evento) {
        if (emailDaEquipe == null || emailDaEquipe.isBlank()) {
            log.warn("Aviso de curso aguardando revisão sem destinatário configurado (curso {}).", evento.cursoId());
        }
        ModeloDeEmail modelo = ModeloDeEmail.com("Curso aguardando revisão")
                .saudacao("Olá, equipe TechMentor!")
                .paragrafo("Uma empresa parceira enviou um curso para revisão.")
                .detalhe("Curso", evento.tituloDoCurso())
                .detalhe("Parceira", evento.nomeDaParceira())
                .detalhe("Identificação", "#" + evento.cursoId())
                .paragrafo("Acesse a fila de revisão no aplicativo para analisar o conteúdo.");

        enviar("curso-enviado-revisao:" + evento.eventoId(), emailDaEquipe == null ? "" : emailDaEquipe,
                "Curso aguardando revisão - TechMentor", modelo, true);
    }

    /** A aprovação já publica o curso na regra do backend; um e-mail confirma as duas coisas sem duplicar avisos. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoRevisarCurso(CursoRevisado evento) {
        String titulo;
        ModeloDeEmail modelo;
        if (evento.decisao() == DecisaoDeRevisao.APROVADO) {
            titulo = "Curso aprovado e publicado";
            modelo = ModeloDeEmail.com(titulo)
                    .saudacao("Olá, " + evento.parceira().getNome() + "!")
                    .paragrafo("A equipe aprovou seu curso e ele já está publicado no catálogo do TechMentor.")
                    .detalhe("Curso", evento.tituloDoCurso());
        } else if (evento.decisao() == DecisaoDeRevisao.DEVOLVIDO) {
            titulo = "Ajustes necessários no seu curso";
            modelo = ModeloDeEmail.com(titulo)
                    .saudacao("Olá, " + evento.parceira().getNome() + "!")
                    .paragrafo("A equipe revisou seu curso e o devolveu para ajustes. Depois de atualizar o conteúdo, você poderá enviá-lo novamente.")
                    .detalhe("Curso", evento.tituloDoCurso())
                    .destaque("Comentário da equipe", evento.comentario() == null ? "Consulte o aplicativo para ver os detalhes." : evento.comentario());
        } else {
            titulo = "Atualização sobre seu curso";
            modelo = ModeloDeEmail.com("Curso não aprovado")
                    .saudacao("Olá, " + evento.parceira().getNome() + "!")
                    .paragrafo("A equipe não aprovou este curso. A decisão está registrada no aplicativo.")
                    .detalhe("Curso", evento.tituloDoCurso())
                    .destaque("Motivo informado pela equipe", evento.comentario() == null ? "Consulte o aplicativo para ver os detalhes." : evento.comentario());
        }
        enviar("curso-revisado:" + evento.eventoId(), evento.parceira().getEmail(),
                titulo + " - TechMentor", modelo, true);
    }

    private void enviar(String chave, String destinatario, String assunto, ModeloDeEmail modelo, boolean retentavel) {
        try {
            emailService.enviarRastreado(chave, destinatario, assunto, modelo, retentavel);
        } catch (Exception e) {
            // A notificação da tela/auditoria não é desfeita por uma indisponibilidade do provedor de e-mail.
            log.warn("Não foi possível preparar o e-mail de parceria {} ({}).", chave, e.getClass().getSimpleName());
        }
    }
}
