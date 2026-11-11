package com.techmentor.api.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * O corpo de TODA resposta de erro da API.
 * "erro" é a mensagem para a pessoa ler; "codigo" é para o app decidir o que fazer;
 * "campos" (só em validação) diz qual campo do formulário errou.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // campos == null não aparece no JSON
public record ErroApi(String erro, String codigo, Map<String, String> campos) {

    public static ErroApi de(HttpStatus status, String mensagem) {
        return new ErroApi(mensagem, codigoPadrao(status), null);
    }

    public static ErroApi deValidacao(String mensagem, Map<String, String> campos) {
        return new ErroApi(mensagem, "DADOS_INVALIDOS", campos);
    }

    /** JSON escrito à mão, para quem responde fora do Spring MVC (os filtros do Spring Security). */
    public String paraJson() {
        return "{\"erro\":\"" + mensagem(erro) + "\",\"codigo\":\"" + codigo + "\"}";
    }

    private static String mensagem(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String codigoPadrao(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "REQUISICAO_INVALIDA";
            case UNAUTHORIZED -> "NAO_AUTENTICADO";
            case FORBIDDEN -> "ACESSO_NEGADO";
            case NOT_FOUND -> "NAO_ENCONTRADO";
            case CONFLICT -> "CONFLITO";
            case TOO_MANY_REQUESTS -> "MUITAS_REQUISICOES";
            default -> "ERRO";
        };
    }
}
