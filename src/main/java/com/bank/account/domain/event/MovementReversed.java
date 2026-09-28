package com.bank.account.domain.event;

import com.bank.account.domain.model.ReversalResult;

public record MovementReversed(String operationId, String accountId, java.math.BigDecimal newBalance)
        implements AccountDomainEvent {

    public static MovementReversed from(ReversalResult result) {
        return new MovementReversed(result.operationId(), result.accountId().value(), result.newBalance().amount());
    }
}
