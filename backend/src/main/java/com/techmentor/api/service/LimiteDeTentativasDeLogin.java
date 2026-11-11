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
 * Limite de senhas erradas seguidas ao entrar: depois de N erros do mesmo e-mail, a entrada daquele e-mail fica
 * bloqueada por alguns minutos (mesmo com a senha certa). Isso impede alguém de tentar senhas à vontade.
 *
 * - A contagem é por e-mail e vale só dentro de uma janela de tempo: erros antigos não somam para sempre.
 * - Erro conta mesmo para e-mail que não existe, para o bloqueio não revelar quem tem conta.
 * - Fica na memória do servidor: reiniciar o backend zera os bloqueios (para este projeto, uma instância só, basta).
 * - Entrar com sucesso zera a contagem.
 *
 * Os valores vêm do application.properties: app.login.max-tentativas e app.login.bloqueio-minutos.
 */
@Component
public class LimiteDeTentativasDeLogin {

    /** Quantas falhas seguidas, quando a janela da contagem começou e, se já estourou, até quando está bloqueado. */
    private record Registro(int falhas, Instant inicioDaContagem, Instant bloqueadoAte) { }

    // Para a memória não crescer sem fim com e-mails inventados, os registros vencidos são varridos acima deste tamanho
    private static final int TAMANHO_PARA_LIMPAR = 10_000;

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maximoDeFalhas;
    private final Duration janela;
    private final Duration bloqueio;
    private final Clock relogio;

    @Autowired
    public LimiteDeTentativasDeLogin(@Value("${app.login.max-tentativas:5}") int maximoDeFalhas,
                                     @Value("${app.login.bloqueio-minutos:15}") int bloqueioEmMinutos) {
        this(maximoDeFalhas, Duration.ofMinutes(bloqueioEmMinutos), Duration.ofMinutes(bloqueioEmMinutos), Clock.systemDefaultZone());
    }

    /** Usado nos testes, para controlar o relógio. */
    LimiteDeTentativasDeLogin(int maximoDeFalhas, Duration janela, Duration bloqueio, Clock relogio) {
        this.maximoDeFalhas = maximoDeFalhas;
        this.janela = janela;
        this.bloqueio = bloqueio;
        this.relogio = relogio;
    }

    /** Chame antes de conferir a senha. Se o e-mail está bloqueado, responde 429 com o tempo que falta. */
    public void exigirLivre(String email) {
        Registro registro = registros.get(email);
        if (registro == null || registro.bloqueadoAte() == null) return;

        Instant agora = relogio.instant();
        if (agora.isBefore(registro.bloqueadoAte())) {
            long segundos = Duration.between(agora, registro.bloqueadoAte()).toSeconds();
            long minutos = Math.max(1, (segundos + 59) / 60);
            throw new TooManyRequestsException("Muitas tentativas de entrada. Tente de novo em "
                    + minutos + (minutos == 1 ? " minuto." : " minutos."));
        }

        // o bloqueio já passou
        registros.remove(email, registro);
    }

    /** Chame quando a senha estiver errada (ou a conta não existir). */
    public void registrarFalha(String email) {
        Instant agora = relogio.instant();

        registros.compute(email, (chave, atual) -> {
            boolean recomecar = atual == null
                    || (atual.bloqueadoAte() != null && !agora.isBefore(atual.bloqueadoAte()))
                    || (atual.bloqueadoAte() == null && agora.isAfter(atual.inicioDaContagem().plus(janela)));

            Registro base = recomecar ? new Registro(0, agora, null) : atual;
            int falhas = base.falhas() + 1;
            Instant bloqueadoAte = falhas >= maximoDeFalhas ? agora.plus(bloqueio) : base.bloqueadoAte();

            return new Registro(falhas, base.inicioDaContagem(), bloqueadoAte);
        });

        if (registros.size() > TAMANHO_PARA_LIMPAR) {
            varrerVencidos(agora);
        }
    }

    /** Chame quando a pessoa entrou: zera a contagem dela. */
    public void limpar(String email) {
        registros.remove(email);
    }

    private void varrerVencidos(Instant agora) {
        registros.entrySet().removeIf(entrada -> {
            Registro r = entrada.getValue();
            return r.bloqueadoAte() != null
                    ? !agora.isBefore(r.bloqueadoAte())
                    : agora.isAfter(r.inicioDaContagem().plus(janela));
        });
    }
}
