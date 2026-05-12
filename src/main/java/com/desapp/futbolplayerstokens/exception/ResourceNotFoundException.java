package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseAppException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, cause);
    }
}
