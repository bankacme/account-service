package com.bank.account.domain.model;

public record AccountConditions(
        Money maintenanceFee,
        Money minimumOpeningAmount,
        Integer monthlyMovementLimit,
        int freeTransactionsLimit,
        Money transactionFee,
        Money minimumDailyAverage,
        boolean requiresCreditCard) {

    public AccountConditions {
        if (maintenanceFee == null || minimumOpeningAmount == null || transactionFee == null) {
            throw new IllegalArgumentException("maintenanceFee, minimumOpeningAmount and transactionFee are required");
        }
        if (monthlyMovementLimit != null && monthlyMovementLimit < 1) {
            throw new IllegalArgumentException("monthlyMovementLimit must be at least 1 when present");
        }
        if (freeTransactionsLimit < 0) {
            throw new IllegalArgumentException("freeTransactionsLimit must not be negative");
        }
    }
}
