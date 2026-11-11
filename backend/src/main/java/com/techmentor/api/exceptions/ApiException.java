package com.techmentor.api.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Base de todos os erros "esperados" da API (400, 401, 403, 404, 409, 429...).
 * Não se lança direto: use a exceção do caso (NotFoundException, ConflictException, BadRequestException...).
 * Ela só guarda o status HTTP; o ApiExceptionHandler transforma qualquer filha na resposta de erro padrão.
 */
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
