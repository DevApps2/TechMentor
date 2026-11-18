package com.techmentor.api.model;

/** Em que pé está um pedido de parceria feito pelo site. */
public enum SituacaoPedido {
    /** Chegou e espera a análise da administradora */
    PENDENTE,
    /** Aprovado: a conta de parceira foi criada e o convite saiu por e-mail */
    APROVADO,
    /** Recusado, com o motivo guardado */
    RECUSADO
}
