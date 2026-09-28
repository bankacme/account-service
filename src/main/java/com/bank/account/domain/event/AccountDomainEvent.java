package com.bank.account.domain.event;

public sealed interface AccountDomainEvent
        permits AccountCreated, AccountUpdated, AccountClosed, MovementApplied, MovementRejected,
        MovementReversed, MovementReversalRejected {
}
