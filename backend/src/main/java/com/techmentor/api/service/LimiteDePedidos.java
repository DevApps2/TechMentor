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
 * Limite de pedidos de parceria por endereço de IP: o formulário do site é público, então sem isso qualquer robô
 * enche a fila da administradora. Depois de N pedidos na última hora, o endereço recebe 429.
 *
 * Mesmas regras do limite de cadastro: janela de 1 hora a partir do primeiro pedido, toda tentativa conta
 * (inclusive as que falham na validação) e a contagem fica na memória (reiniciar a API zera).
 * O valor vem de app.pedidos.max-por-hora.
 */
@Component
public class LimiteDePedidos {

    private record Registro(int total, Instant inicio) { }

    private static final int TAMANHO_PARA_LIMPAR = 10_000;

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maximo;
    private final Duration janela;
    private final Clock relogio;

    @Autowired
    public LimiteDePedidos(@Value("${app.pedidos.max-por-hora:5}") int maximo) {
        this(maximo, Duration.ofHours(1), Clock.systemDefaultZone());
    }

    /** Usado nos testes, para controlar o relógio. */
    LimiteDePedidos(int maximo, Duration janela, Clock relogio) {
        this.maximo = maximo;
        this.janela = janela;
        this.relogio = relogio;
    }

    /** Chame antes de processar o pedido. Se este endereço já mandou o máximo na última hora, responde 429. */
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
            throw new TooManyRequestsException("Você enviou pedidos demais em pouco tempo. Tente de novo mais tarde.");
        }
    }
}
