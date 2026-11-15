package com.techmentor.api.model;

/**
 * CONVIDADA: recebeu o convite (e talvez já aceitou), mas a administradora ainda não verificou. Não cria curso.
 * ATIVA: verificada; monta cursos e envia para revisão.
 * SUSPENSA: não altera mais nada; o que já está publicado segue no ar até a administradora decidir.
 */
public enum SituacaoParceira {
    CONVIDADA, ATIVA, SUSPENSA
}
