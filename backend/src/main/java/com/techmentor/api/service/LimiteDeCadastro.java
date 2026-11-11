package com.techmentor.api.service;

import com.techmentor.api.exceptions.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de cadastros por endereço de IP: depois de N tentativas na última hora, o endereço recebe 429.
 * Impede criar contas em massa (spam, lixo no banco) e também testar milhares de e-mails para descobrir quem já tem conta.
 *
 * - A contagem é por IP e vale por uma janela de 1 hora, a partir da primeira tentativa.
 * - Toda tentativa conta, mesmo as que falham (e-mail repetido, por exemplo).
 * - Fica na memória do servidor: reiniciar o backend zera os limites (uma instância só, basta para este projeto).
 *
 * O valor vem do application.properties: app.cadastro.max-por-hora.
 */
@Component
public class LimiteDeCadastro {

    private record Registro(int total, Instant inicio) { }

    // Para a memória não crescer sem fim com endereços inventados, os registros vencidos são varridos acima deste tamanho
    private static final int TAMANHO_PARA_LIMPAR = 10_000;

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maximo;
    private final Duration janela;
    private final Clock relogio;

    @Autowired
    public LimiteDeCadastro(@Value("${app.cadastro.max-por-hora:10}") int maximo) {
        this(maximo, Duration.ofHours(1), Clock.systemDefaultZone());
    }

    /** Usado nos testes, para controlar o relógio. */
    LimiteDeCadastro(int maximo, Duration janela, Clock relogio) {
        this.maximo = maximo;
        this.janela = janela;
        this.relogio = relogio;
    }

    /** Chame antes de criar a conta. Se este endereço já tentou o máximo na última hora, responde 429. */
    public void registrar(String ip) {
        Instant agora = relogio.instant();
        boolean[] estourou = {false};

        registros.compute(ip, (chave, atual) -> {
            Registro base = (atual == null || agora.isAfter(atual.inicio().plus(janela)))
                    ? new Registro(0, agora)
                    : atual;

            if (base.total() >= maximo) {
                estourou[0] = true;
                return base;
            }
            return new Registro(base.total() + 1, base.inicio());
        });

        if (registros.size() > TAMANHO_PARA_LIMPAR) {
            registros.entrySet().removeIf(e -> agora.isAfter(e.getValue().inicio().plus(janela)));
        }

        if (estourou[0]) {
            throw new TooManyRequestsException("Muitos cadastros deste dispositivo. Tente de novo mais tarde.");
        }
    }
}
