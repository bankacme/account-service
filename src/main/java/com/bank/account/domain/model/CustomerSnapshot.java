package com.bank.account.domain.model;

public record CustomerSnapshot(String customerId, CustomerType type, CustomerProfile profile, String status) {

    public CustomerSnapshot {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        if (type == null || profile == null || status == null) {
            throw new IllegalArgumentException("type, profile and status are required");
        }
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
