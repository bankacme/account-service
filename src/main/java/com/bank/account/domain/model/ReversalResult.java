package com.bank.account.domain.model;

public record ReversalResult(String operationId, AccountId accountId, Money newBalance) {

    public ReversalResult {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (accountId == null || newBalance == null) {
            throw new IllegalArgumentException("accountId and newBalance are required");
        }
    }
}
