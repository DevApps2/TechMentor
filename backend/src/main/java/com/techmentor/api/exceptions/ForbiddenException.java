package com.techmentor.api.exceptions;

import org.springframework.http.HttpStatus;

/** 403: está autenticada, mas não pode fazer isso. */
public class ForbiddenException extends ApiException {
    public ForbiddenException() {
        this("Acesso não permitido.");
    }

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
