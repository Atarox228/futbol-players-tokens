package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

public class DataUpdateException extends BaseAppException {
    public DataUpdateException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public DataUpdateException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
