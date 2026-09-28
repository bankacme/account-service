package com.bank.account.domain.exception;

public class ConditionsNotFoundException extends RuntimeException {

    public ConditionsNotFoundException(String productId) {
        super("Account conditions not found: " + productId);
    }
}
