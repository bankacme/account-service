package com.bank.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BalanceTrackerTest {

    @Test
    void matchesTheWorkedExampleFromDataModelSection33() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        BalanceTracker tracker = BalanceTracker.initial(Money.of(new BigDecimal("1000.00")), day1);

        LocalDate day11 = LocalDate.of(2026, 9, 11);
        tracker = tracker.recordChange(Money.of(new BigDecimal("400.00")), day11);

        LocalDate day21 = LocalDate.of(2026, 9, 21);
        BigDecimal average = tracker.dailyAverage(day21, day1);

        assertThat(average).isEqualByComparingTo("685.71");
    }

    @Test
    void withNoMovementsTheAverageIsJustTheOpeningBalance() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        BalanceTracker tracker = BalanceTracker.initial(Money.of(new BigDecimal("500.00")), day1);

        BigDecimal average = tracker.dailyAverage(LocalDate.of(2026, 9, 10), day1);

        assertThat(average).isEqualByComparingTo("500.00");
    }

    @Test
    void rollsOverToZeroWhenQueriedInALaterMonth() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        BalanceTracker tracker = BalanceTracker.initial(Money.of(new BigDecimal("500.00")), day1);

        // No movement happened in October, but the balance (500) is still what's "held".
        BigDecimal average = tracker.dailyAverage(LocalDate.of(2026, 10, 5), day1);

        assertThat(average).isEqualByComparingTo("500.00");
    }
}
