package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BaseAppException {
    public InsufficientStockException(String playerName, int requested, int available) {
        super(String.format("Insufficient stock for player '%s': requested %d, available %d",
                playerName, requested, available), HttpStatus.CONFLICT);
    }
}
