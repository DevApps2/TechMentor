package com.techmentor.api.exceptions;

import org.springframework.http.HttpStatus;

/** 404: o registro pedido não existe. */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
