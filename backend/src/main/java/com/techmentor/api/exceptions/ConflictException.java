package com.techmentor.api.exceptions;

import org.springframework.http.HttpStatus;

/** 409: o pedido conflita com o estado atual (já existe, já foi feito, não dá mais para mudar). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
