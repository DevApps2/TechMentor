package com.techmentor.api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NivelRegraServiceTest {

    @Test
    void nivelSobeACada250Xp() {
        assertEquals(1, NivelRegraService.nivelPara(0));
        assertEquals(1, NivelRegraService.nivelPara(249));
        assertEquals(2, NivelRegraService.nivelPara(250));
        assertEquals(5, NivelRegraService.nivelPara(1_000));
    }

    @Test
    void proximoNivelEXpFaltante() {
        assertEquals(250, NivelRegraService.xpProximoNivel(0));
        assertEquals(250, NivelRegraService.xpFaltante(0));
        assertEquals(500, NivelRegraService.xpProximoNivel(260));
        assertEquals(240, NivelRegraService.xpFaltante(260));
    }

    @Test
    void tituloDoNivel() {
        assertEquals("Curiosa Tech", NivelRegraService.titulo(1));
        assertEquals("Aprendiz Tech", NivelRegraService.titulo(3));
        assertEquals("Exploradora Tech", NivelRegraService.titulo(7));
        assertEquals("Referência Tech", NivelRegraService.titulo(20));
    }

    @Test
    void sequenciaZeraSeHouveUmDiaSemAtividade() {
        LocalDate hoje = LocalDate.of(2026, 10, 28);
        assertEquals(5, NivelRegraService.sequenciaAtual(5, hoje, hoje));
        assertEquals(5, NivelRegraService.sequenciaAtual(5, hoje.minusDays(1), hoje));
        assertEquals(0, NivelRegraService.sequenciaAtual(5, hoje.minusDays(2), hoje));
        assertEquals(0, NivelRegraService.sequenciaAtual(5, null, hoje));
    }
}
