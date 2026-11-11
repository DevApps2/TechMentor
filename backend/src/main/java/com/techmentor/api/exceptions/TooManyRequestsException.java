package com.techmentor.api.exceptions;

import org.springframework.http.HttpStatus;

/** 429: passou do limite de tentativas ou de pedidos em um período. */
public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
