package com.techmentor.api.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O CORS só compara a origem (esquema + domínio + porta): caminho e barra final não podem derrubar um site autorizado. */
class OrigemDoCorsTest {

    @Test
    void tiraOCaminhoEABarraFinal() {
        assertEquals("https://devapps2.github.io", SecurityConfig.soAOrigem("https://devapps2.github.io/TechMentor-Site"));
        assertEquals("https://devapps2.github.io", SecurityConfig.soAOrigem("  https://devapps2.github.io/TechMentor-Site/  "));
        assertEquals("https://devapps2.github.io", SecurityConfig.soAOrigem("https://devapps2.github.io/"));
    }

    @Test
    void mantemAsOrigensQueJaEstavamCertas() {
        assertEquals("http://localhost:5173", SecurityConfig.soAOrigem("http://localhost:5173"));
        assertEquals("https://techmentor-portal.vercel.app", SecurityConfig.soAOrigem("https://techmentor-portal.vercel.app"));
    }

    @Test
    void textoVazioContinuaVazioParaSerDescartado() {
        assertEquals("", SecurityConfig.soAOrigem("   "));
    }
}
