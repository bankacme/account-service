package com.bank.account.domain.exception;

public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String operationId, String accountId) {
        super("Operation " + operationId + " not found for account " + accountId);
    }
}
