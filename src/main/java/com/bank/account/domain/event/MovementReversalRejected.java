package com.bank.account.domain.event;

public record MovementReversalRejected(String operationId, String accountId, String reasonCode)
        implements AccountDomainEvent {
}
