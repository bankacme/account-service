package com.bank.account.domain.model;

public record MovementResult(
        String operationId,
        AccountId accountId,
        MovementType type,
        Money amount,
        Money fee,
        Money newBalance,
        int movementNumber) {

    public MovementResult {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (accountId == null || type == null || amount == null || fee == null || newBalance == null) {
            throw new IllegalArgumentException("All MovementResult fields are required");
        }
        if (movementNumber < 1) {
            throw new IllegalArgumentException("movementNumber must be at least 1");
        }
    }
}
