package com.bank.account.domain.model;

import java.time.Clock;
import java.time.Instant;

public record AccountProduct(
        ProductId id, AccountType accountType, CustomerProfile profile, AccountConditions conditions,
        Instant updatedAt) {

    public AccountProduct {
        if (id == null || accountType == null || profile == null || conditions == null || updatedAt == null) {
            throw new IllegalArgumentException("All AccountProduct fields are required");
        }
    }

    public static AccountProduct create(AccountType accountType, CustomerProfile profile,
                                         AccountConditions conditions, Clock clock) {
        return new AccountProduct(ProductId.of(accountType, profile), accountType, profile, conditions,
                clock.instant());
    }

    public AccountProduct withConditions(AccountConditions newConditions, Clock clock) {
        return new AccountProduct(id, accountType, profile, newConditions, clock.instant());
    }
}
