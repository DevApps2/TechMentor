package com.techmentor.api.sala;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GeradorDeSala {

    /** Cria a sala. Devolve vazio se o Google não estiver configurado ou se der erro (nunca lança exceção). */
    Optional<SalaCriada> criar(DadosDaSala dados);

    /** Apaga o evento (e a sala) e avisa os convidados. Não lança exceção. */
    void apagar(String idDoEvento);

    record DadosDaSala(String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim, List<String> emails) { }

    record SalaCriada(String link, String idDoEvento) { }
}