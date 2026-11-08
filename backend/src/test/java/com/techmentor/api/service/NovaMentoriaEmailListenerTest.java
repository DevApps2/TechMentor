package com.techmentor.api.service;

import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.evento.EventosDeNotificacao.MentoriaAgendada;
import com.techmentor.api.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** O e-mail que a mentora recebe quando uma aluna pede um horário. */
class NovaMentoriaEmailListenerTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 10, 14, 0);

    private EmailService emailService;
    private NovaMentoriaEmailListener listener;

    @BeforeEach
    void preparar() {
        emailService = mock(EmailService.class);
        listener = new NovaMentoriaEmailListener(emailService);
    }

    private MentoriaAgendada evento(String assunto) {
        return new MentoriaAgendada(new Usuario(), "Ana Souza", INICIO,
                "Maria Silva", "maria@teste.com", assunto);
    }

    @Test
    void enviaParaAMentoraComOsDadosDaSolicitacao() {
        listener.aoSolicitar(evento("Dúvidas de carreira"));

        ArgumentCaptor<String> para = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> assunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ModeloDeEmail> modelo = ArgumentCaptor.forClass(ModeloDeEmail.class);
        verify(emailService).enviar(para.capture(), assunto.capture(), modelo.capture());

        assertEquals("maria@teste.com", para.getValue());
        assertTrue(assunto.getValue().contains("Nova solicitação de mentoria"), assunto.getValue());
        assertTrue(assunto.getValue().contains("10/11 às 14:00"), assunto.getValue());

        String texto = modelo.getValue().texto();
        assertTrue(texto.contains("Maria Silva"), texto);
        assertTrue(texto.contains("Ana Souza"), texto);
        assertTrue(texto.contains("Dúvidas de carreira"), texto);
        assertTrue(texto.contains("cancelada automaticamente"), texto);
    }

    @Test
    void semAssuntoOEmailNaoMostraOCampo() {
        listener.aoSolicitar(evento("   "));

        ArgumentCaptor<ModeloDeEmail> modelo = ArgumentCaptor.forClass(ModeloDeEmail.class);
        verify(emailService).enviar(anyString(), anyString(), modelo.capture());

        assertFalse(modelo.getValue().texto().contains("Assunto"), modelo.getValue().texto());
    }

    @Test
    void falhaNoEnvioNaoDerrubaQuemAgendou() {
        doThrow(new RuntimeException("Gmail fora do ar"))
                .when(emailService).enviar(anyString(), anyString(), any(ModeloDeEmail.class));

        assertDoesNotThrow(() -> listener.aoSolicitar(evento("Dúvidas de carreira")));
    }
}