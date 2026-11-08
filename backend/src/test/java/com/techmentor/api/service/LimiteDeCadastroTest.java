package com.techmentor.api.service;

import com.techmentor.api.exceptions.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** No máximo N cadastros por hora vindos do mesmo endereço. */
class LimiteDeCadastroTest {

    private static class RelogioDeTeste extends Clock {
        private Instant agora = Instant.parse("2026-11-08T12:00:00Z");

        void avancar(Duration tempo) { agora = agora.plus(tempo); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @Test
    void depoisDoMaximoOEnderecoRecebe429() {
        LimiteDeCadastro limite = new LimiteDeCadastro(3, Duration.ofHours(1), new RelogioDeTeste());

        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limite.registrar("1.1.1.1"));
        }

        ApiException erro = assertThrows(ApiException.class, () -> limite.registrar("1.1.1.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatus());
    }

    @Test
    void cadaEnderecoTemASuaPropriaContagem() {
        LimiteDeCadastro limite = new LimiteDeCadastro(1, Duration.ofHours(1), new RelogioDeTeste());

        limite.registrar("1.1.1.1");

        assertThrows(ApiException.class, () -> limite.registrar("1.1.1.1"));
        assertDoesNotThrow(() -> limite.registrar("2.2.2.2"));
    }

    @Test
    void passadaAJanelaOEnderecoPodeCadastrarDeNovo() {
        RelogioDeTeste relogio = new RelogioDeTeste();
        LimiteDeCadastro limite = new LimiteDeCadastro(1, Duration.ofHours(1), relogio);

        limite.registrar("1.1.1.1");
        assertThrows(ApiException.class, () -> limite.registrar("1.1.1.1"));

        relogio.avancar(Duration.ofHours(1).plusMinutes(1));

        assertDoesNotThrow(() -> limite.registrar("1.1.1.1"));
    }
}
