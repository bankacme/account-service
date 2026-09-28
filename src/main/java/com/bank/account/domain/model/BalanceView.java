package com.bank.account.domain.model;

import java.time.Instant;

public record BalanceView(String accountId, Money balance, Instant asOf, DailyAverage dailyAverage) {

    public BalanceView {
        if (accountId == null || accountId.isBlank() || balance == null || asOf == null) {
            throw new IllegalArgumentException("accountId, balance and asOf are required");
        }
    }
}
