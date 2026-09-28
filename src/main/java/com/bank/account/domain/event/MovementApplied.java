package com.bank.account.domain.event;

import com.bank.account.domain.model.MovementResult;

public record MovementApplied(
        String operationId, String accountId, String type, java.math.BigDecimal amount,
        java.math.BigDecimal fee, java.math.BigDecimal newBalance, int movementNumber)
        implements AccountDomainEvent {

    public static MovementApplied from(MovementResult result) {
        return new MovementApplied(
                result.operationId(),
                result.accountId().value(),
                result.type().name(),
                result.amount().amount(),
                result.fee().amount(),
                result.newBalance().amount(),
                result.movementNumber());
    }
}
