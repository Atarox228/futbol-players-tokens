package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

public class ConfigurationException extends BaseAppException {
    public ConfigurationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
