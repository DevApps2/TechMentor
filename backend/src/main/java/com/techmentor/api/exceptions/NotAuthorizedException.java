package com.techmentor.api.exceptions;

import org.springframework.http.HttpStatus;

/** 401: quem chamou não está autenticada (sem token, token vencido ou conta que não existe mais). */
public class NotAuthorizedException extends ApiException {
    public NotAuthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
