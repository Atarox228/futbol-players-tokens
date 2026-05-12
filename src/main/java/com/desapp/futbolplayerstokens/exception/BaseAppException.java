package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseAppException extends RuntimeException {
    private final HttpStatus httpStatus;

    public BaseAppException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public BaseAppException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
