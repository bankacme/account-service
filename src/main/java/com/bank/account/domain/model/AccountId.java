package com.bank.account.domain.model;

import java.util.UUID;

public record AccountId(String value) {

    public AccountId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AccountId must not be blank");
        }
    }

    public static AccountId newId() {
        return new AccountId(UUID.randomUUID().toString());
    }
}
