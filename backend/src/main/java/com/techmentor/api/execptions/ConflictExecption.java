package com.techmentor.api.execptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictExecption extends RuntimeException {
    public ConflictExecption(String message) {
        super(message);
    }
}
