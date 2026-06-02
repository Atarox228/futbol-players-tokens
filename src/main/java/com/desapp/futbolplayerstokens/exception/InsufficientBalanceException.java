package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BaseAppException {
    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super(String.format("Insufficient balance: required %.2f, available %.2f",
                required, available), HttpStatus.CONFLICT);
    }
}
