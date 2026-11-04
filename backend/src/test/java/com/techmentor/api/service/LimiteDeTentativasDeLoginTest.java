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
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteDeTentativasDeLoginTest {

    /** Relógio que só anda quando o teste manda, para não esperar minutos de verdade. */
    private static class RelogioDeTeste extends Clock {
        private Instant agora = Instant.parse("2026-11-04T12:00:00Z");

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private final RelogioDeTeste relogio = new RelogioDeTeste();

    // 3 erros seguidos bloqueiam; a contagem e o bloqueio duram 15 minutos
    private final LimiteDeTentativasDeLogin limite =
            new LimiteDeTentativasDeLogin(3, Duration.ofMinutes(15), Duration.ofMinutes(15), relogio);

    private void errar(String email, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limite.exigirLivre(email);
            limite.registrarFalha(email);
        }
    }

    @Test
    void bloqueiaDepoisDoNumeroMaximoDeFalhasEDizQuantoFalta() {
        errar("ana@email.com", 3);

        ApiException erro = assertThrows(ApiException.class, () -> limite.exigirLivre("ana@email.com"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatus());
        assertTrue(erro.getMessage().contains("15 minutos"), erro.getMessage());

        relogio.avancar(Duration.ofMinutes(10));
        ApiException depois = assertThrows(ApiException.class, () -> limite.exigirLivre("ana@email.com"));
        assertTrue(depois.getMessage().contains("5 minutos"), depois.getMessage());

        relogio.avancar(Duration.ofMinutes(4).plusSeconds(30));
        ApiException quase = assertThrows(ApiException.class, () -> limite.exigirLivre("ana@email.com"));
        assertTrue(quase.getMessage().contains("1 minuto."), quase.getMessage());
    }

    @Test
    void naoBloqueiaAntesDoMaximo() {
        errar("ana@email.com", 2);

        assertDoesNotThrow(() -> limite.exigirLivre("ana@email.com"));
    }

    @Test
    void liberaDepoisDoTempoDeBloqueioERecomecaAContagem() {
        errar("ana@email.com", 3);

        relogio.avancar(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> limite.exigirLivre("ana@email.com"));

        // a contagem recomeçou do zero: um erro novo não bloqueia de novo
        limite.registrarFalha("ana@email.com");
        assertDoesNotThrow(() -> limite.exigirLivre("ana@email.com"));
    }

    @Test
    void entrarComSucessoZeraAContagem() {
        errar("ana@email.com", 2);
        limite.limpar("ana@email.com");

        // sem o limpar, o terceiro erro (2 + 1) bloquearia; com ele, são só 2 erros novos
        errar("ana@email.com", 2);

        assertDoesNotThrow(() -> limite.exigirLivre("ana@email.com"));
    }

    @Test
    void errosAntigosForaDaJanelaNaoSomam() {
        errar("ana@email.com", 2);

        relogio.avancar(Duration.ofMinutes(16));
        errar("ana@email.com", 2);

        assertDoesNotThrow(() -> limite.exigirLivre("ana@email.com"));
    }

    @Test
    void cadaEmailTemASuaContagem() {
        errar("ana@email.com", 3);

        assertThrows(ApiException.class, () -> limite.exigirLivre("ana@email.com"));
        assertDoesNotThrow(() -> limite.exigirLivre("bia@email.com"));
    }

    @Test
    void emailQueNaoExisteTambemConta() {
        // o bloqueio não pode revelar quem tem conta: e-mail inventado é bloqueado do mesmo jeito
        errar("ninguem@email.com", 3);

        assertThrows(ApiException.class, () -> limite.exigirLivre("ninguem@email.com"));
    }
}
