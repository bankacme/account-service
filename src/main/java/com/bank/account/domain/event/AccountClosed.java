package com.bank.account.domain.event;

import com.bank.account.domain.model.Account;
import java.time.Instant;

public record AccountClosed(
        String accountId, String maskedNumber, String customerId, String type, String status, Instant occurredAt)
        implements AccountDomainEvent {

    public static AccountClosed from(Account account) {
        return new AccountClosed(
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
