package com.bank.account.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccountConditionsTest {

    @Test
    void rejectsAMonthlyMovementLimitBelowOne() {
        assertThatThrownBy(() -> new AccountConditions(
                Money.zero(), Money.zero(), 0, 5, Money.zero(), null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeFreeTransactionsLimit() {
        assertThatThrownBy(() -> new AccountConditions(
                Money.zero(), Money.zero(), null, -1, Money.zero(), null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
