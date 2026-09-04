package com.elmlite.platform.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = Objects.requireNonNull(status);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
