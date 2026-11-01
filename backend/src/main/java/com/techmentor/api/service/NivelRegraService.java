package com.techmentor.api.service;

import java.time.LocalDate;


import java.time.LocalDate;

/** Regras de nível e sequência de dias (Home e Conquistas do app). */
public final class NivelRegraService {

    /** XP necessário para subir cada nível: nível 1 de 0 a 249, nível 2 de 250 a 499, e assim por diante. */
    public static final int XP_POR_NIVEL = 250;

    /** Último nível que existe. Depois dele o XP continua somando, mas o nível não sobe mais. */
    public static final int NIVEL_MAXIMO = 100;

    // Cada título vale até o nível indicado em LIMITES; o último título é o do nível máximo.
    // TITULOS tem um item a mais que LIMITES, justamente esse último.
    private static final int[] LIMITES = {2, 5, 9, 14, 19, 29, 39, 49, 59, 69, 79, 89, 99};
    private static final String[] TITULOS = {
            "Curiosa Tech",        // 1 a 2
            "Aprendiz Tech",       // 3 a 5
            "Exploradora Tech",    // 6 a 9
            "Construtora Tech",    // 10 a 14
            "Criadora Tech",       // 15 a 19
            "Inovadora Tech",      // 20 a 29
            "Estrategista Tech",   // 30 a 39
            "Especialista Tech",   // 40 a 49
            "Mentora Tech",        // 50 a 59
            "Líder Tech",          // 60 a 69
            "Visionária Tech",     // 70 a 79
            "Pioneira Tech",       // 80 a 89
            "Referência Tech",     // 90 a 99
            "Lenda Tech"           // 100
    };

    private NivelRegraService() { }

    public static int nivelPara(int xpTotal) {
        return Math.min(NIVEL_MAXIMO, Math.max(0, xpTotal) / XP_POR_NIVEL + 1);
    }

    /** XP total em que o próximo nível começa. */
    public static int xpProximoNivel(int xpTotal) {
        return nivelPara(xpTotal) * XP_POR_NIVEL;
    }

    /** Quanto falta para o próximo nível; no nível máximo não falta nada. */
    public static int xpFaltante(int xpTotal) {
        return Math.max(0, xpProximoNivel(xpTotal) - Math.max(0, xpTotal));
    }

    public static String titulo(int nivel) {
        for (int i = 0; i < LIMITES.length; i++) {
            if (nivel <= LIMITES[i]) return TITULOS[i];
        }
        return TITULOS[TITULOS.length - 1];
    }

    /**
     * Sequência que vale hoje: se a última atividade foi antes de ontem a sequência foi interrompida
     * e volta a zero (o valor gravado só é atualizado quando a usuária ganha XP de novo).
     */
    public static int sequenciaAtual(int sequenciaGravada, LocalDate ultimaAtividade, LocalDate hoje) {
        if (ultimaAtividade == null) return 0;
        return ultimaAtividade.isBefore(hoje.minusDays(1)) ? 0 : sequenciaGravada;
    }
}