package com.bank.account.domain.model;

public record ProductId(String value) {

    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId must not be blank");
        }
    }

    public static ProductId of(AccountType type, CustomerProfile profile) {
        return new ProductId(type.name() + "_" + profile.name());
    }

    public static ProductId standardOf(AccountType type) {
        return of(type, CustomerProfile.STANDARD);
    }
}
