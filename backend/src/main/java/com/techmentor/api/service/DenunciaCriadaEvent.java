package com.techmentor.api.service;

import java.time.LocalDateTime;

/**
 * Aviso interno de que uma denúncia foi registrada. Leva só textos e números (nada de entidades),
 * porque quem escuta roda depois que a transação terminou.
 */
public record DenunciaCriadaEvent(
        Long id,
        String motivo,
        String descricao,
        LocalDateTime criadaEm,
        Long denuncianteId,
        String denuncianteNome,
        Long denunciadoId,
        String denunciadoNome) { }
