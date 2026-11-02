package com.techmentor.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Chama o LembreteDeMentoriaService de minuto em minuto.
 * Para desligar (como nos testes): app.lembretes.ativo=false
 */
@Component
@ConditionalOnProperty(name = "app.lembretes.ativo", havingValue = "true", matchIfMissing = true)
public class LembreteAgendado {

    private static final Logger log = LoggerFactory.getLogger(LembreteAgendado.class);

    private final LembreteDeMentoriaService servico;

    public LembreteAgendado(LembreteDeMentoriaService servico) {
        this.servico = servico;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void rodar() {
        try {
            servico.processar();
        } catch (Exception e) {
            // Uma falha não pode derrubar o relógio: a próxima rodada tenta de novo
            log.error("Falha ao processar os lembretes de mentoria: {}", e.getMessage());
        }
    }
}