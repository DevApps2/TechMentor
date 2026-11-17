package com.techmentor.api.service;

/** Confere os dois dígitos verificadores do CNPJ. Não consulta a Receita: só barra número que não pode existir. */
final class Cnpj {

    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() { }

    /** Só os dígitos; texto vazio vira null. */
    static String soDigitos(String texto) {
        if (texto == null) return null;
        String digitos = texto.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    /** Recebe só dígitos (use soDigitos antes). */
    static boolean valido(String digitos) {
        if (digitos == null || digitos.length() != 14) return false;
        if (digitos.chars().distinct().count() == 1) return false;   // 00000000000000, 11111111111111...

        int primeiro = digito(digitos, PESOS_1);
        int segundo = digito(digitos, PESOS_2);
        return primeiro == digitos.charAt(12) - '0' && segundo == digitos.charAt(13) - '0';
    }

    private static int digito(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (digitos.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
