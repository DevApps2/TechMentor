package com.techmentor.api.model;

/** A categoria da notificação: o app usa para escolher o ícone e a cor. */
public enum TipoNotificacao {
    GERAL, CONQUISTA, NIVEL, CURSO, DESAFIO, DENUNCIA, MENTORIA, AVALIACAO, OPORTUNIDADE,
    /** Algo mudou na conta ou no curso de uma empresa parceira */
    PARCERIA,
    /** Um curso de parceira aguarda a análise da administradora */
    REVISAO
}
