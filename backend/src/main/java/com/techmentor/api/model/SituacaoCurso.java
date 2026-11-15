package com.techmentor.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Em que pé está um curso.
 * Só o PUBLICADO aparece no catálogo das alunas; o ARQUIVADO continua acessível para quem já estava matriculada.
 */
@Schema(description = "Situação do curso: RASCUNHO (em montagem), EM_REVISAO (enviado pela parceira, aguardando a administradora), PUBLICADO (visível para as alunas) ou ARQUIVADO (fora do catálogo)")
public enum SituacaoCurso {
    RASCUNHO,
    /** Só cursos de empresas parceiras passam por aqui: a parceira envia, a administradora aprova, devolve ou recusa */
    EM_REVISAO,
    PUBLICADO,
    ARQUIVADO
}
