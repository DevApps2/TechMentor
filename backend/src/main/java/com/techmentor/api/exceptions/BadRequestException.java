package com.techmentor.api.exceptions;

import org.springframework.http.HttpStatus;

/** 400: o pedido está mal formado ou quebra uma regra de entrada. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
