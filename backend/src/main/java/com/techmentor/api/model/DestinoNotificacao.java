package com.techmentor.api.model;

/**
 * Para qual tela o toque na notificação leva. O campo "referencia" da notificação completa o destino quando precisa
 * (o título do curso, o id do curso...). NENHUM = só marca como lida.
 */
public enum DestinoNotificacao {
    NENHUM,
    /** Tela de conquistas da aluna */
    CONQUISTAS,
    /** Trilha do curso; referencia = título do curso */
    CURSO,
    /** Tela de desafios */
    DESAFIOS,
    /** Lista de mentorias (da aluna ou da mentora) */
    MENTORIAS,
    /** Avaliações pendentes ou recebidas */
    AVALIACOES,
    /** Oportunidades e eventos (a aluna vê a situação da candidatura) */
    OPORTUNIDADES,
    /** Denúncias que a pessoa fez */
    DENUNCIAS,
    /** Fila de revisão da administradora; referencia = id do curso */
    REVISAO_DE_CURSO,
    /** Edição do curso pela parceira; referencia = id do curso */
    CURSO_DA_PARCEIRA,
    /** Perfil da parceira (situação da conta) */
    PERFIL_DA_PARCEIRA,
    /** Parceira para a administradora verificar; referencia = id da parceira */
    PARCEIRA_PARA_VERIFICAR
}
