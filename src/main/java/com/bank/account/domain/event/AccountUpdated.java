package com.bank.account.domain.event;

import com.bank.account.domain.model.Account;
import java.time.Instant;

public record AccountUpdated(
        String accountId, String maskedNumber, String customerId, String type, String status, Instant occurredAt)
        implements AccountDomainEvent {

    public static AccountUpdated from(Account account) {
        return new AccountUpdated(
                account.id().value(),
                mask(account.accountNumber().value()),
                account.customerId(),
                account.type().name(),
                account.status().name(),
                account.updatedAt());
    }

    private static String mask(String accountNumber) {
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
