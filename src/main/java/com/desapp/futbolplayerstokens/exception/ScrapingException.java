package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

public class ScrapingException extends BaseAppException {
    public ScrapingException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public ScrapingException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause);
    }
}
