package com.techmentor.api.service;

import java.time.LocalDate;

/** Regras de nível e sequência de dias (Home e Conquistas do app). */
public final class NivelRegraService {

    /** XP necessário para subir cada nível: nível 1 de 0 a 249, nível 2 de 250 a 499, e assim por diante. */
    public static final int XP_POR_NIVEL = 250;

    private NivelRegraService() { }

    public static int nivelPara(int xpTotal) {
        return Math.max(0, xpTotal) / XP_POR_NIVEL + 1;
    }

    /** XP total em que o próximo nível começa. */
    public static int xpProximoNivel(int xpTotal) {
        return nivelPara(xpTotal) * XP_POR_NIVEL;
    }

    public static int xpFaltante(int xpTotal) {
        return xpProximoNivel(xpTotal) - Math.max(0, xpTotal);
    }

    public static String titulo(int nivel) {
        if (nivel <= 2) return "Curiosa Tech";
        if (nivel <= 5) return "Aprendiz Tech";
        if (nivel <= 9) return "Exploradora Tech";
        if (nivel <= 14) return "Construtora Tech";
        return "Referência Tech";
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
