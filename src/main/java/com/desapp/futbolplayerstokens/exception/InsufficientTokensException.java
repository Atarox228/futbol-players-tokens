package com.desapp.futbolplayerstokens.exception;

import org.springframework.http.HttpStatus;

public class InsufficientTokensException extends BaseAppException {
    public InsufficientTokensException(int requested, int owned) {
        super(String.format("Insufficient tokens to sell: requested %d, owned %d",
                requested, owned), HttpStatus.CONFLICT);
    }
}
