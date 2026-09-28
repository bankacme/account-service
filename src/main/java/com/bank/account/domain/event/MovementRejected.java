package com.bank.account.domain.event;

public record MovementRejected(String operationId, String accountId, String reasonCode)
        implements AccountDomainEvent {
}
