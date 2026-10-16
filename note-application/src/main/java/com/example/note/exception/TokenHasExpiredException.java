package com.example.note.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenHasExpiredException extends RuntimeException {

    public TokenHasExpiredException(String message) {
        super(message);
    }
}
