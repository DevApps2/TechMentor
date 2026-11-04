package com.techmentor.api.service;

import java.util.Locale;

/**
 * Um jeito só de escrever e-mail em todo o sistema: sem espaços nas pontas e em minúsculas.
 * Sem isso, "Ana@email.com" e "ana@email.com" seriam duas contas diferentes (ou o login falharia) em bancos que
 * diferenciam maiúsculas de minúsculas, como o H2.
 */
final class Emails {

    private Emails() { }

    static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
