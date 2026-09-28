package com.bank.account.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public record BalanceTracker(
        YearMonth yearMonth, BigDecimal accumulatedBalanceDays, Money lastBalance, LocalDate lastChangeDate) {

    public BalanceTracker {
        if (yearMonth == null || accumulatedBalanceDays == null || lastBalance == null || lastChangeDate == null) {
            throw new IllegalArgumentException("All BalanceTracker fields are required");
        }
    }

    public static BalanceTracker initial(Money openingBalance, LocalDate openingDate) {
        return new BalanceTracker(YearMonth.from(openingDate), BigDecimal.ZERO, openingBalance, openingDate);
    }

    public BalanceTracker recordChange(Money newBalance, LocalDate changeDate) {
        BalanceTracker base = rolledOverTo(YearMonth.from(changeDate));
        long daysHeld = ChronoUnit.DAYS.between(base.lastChangeDate, changeDate);
        BigDecimal accumulated = base.accumulatedBalanceDays
                .add(base.lastBalance.amount().multiply(BigDecimal.valueOf(daysHeld)));
        return new BalanceTracker(base.yearMonth, accumulated, newBalance, changeDate);
    }

    public BigDecimal dailyAverage(LocalDate asOfDate, LocalDate accountOpenedDate) {
        BalanceTracker base = rolledOverTo(YearMonth.from(asOfDate));
        LocalDate monthStart = asOfDate.withDayOfMonth(1);
        LocalDate start = accountOpenedDate.isAfter(monthStart) ? accountOpenedDate : monthStart;
        long days = ChronoUnit.DAYS.between(start, asOfDate) + 1;
        long daysSinceLastChange = ChronoUnit.DAYS.between(base.lastChangeDate, asOfDate) + 1;
        BigDecimal numerator = base.accumulatedBalanceDays
                .add(base.lastBalance.amount().multiply(BigDecimal.valueOf(daysSinceLastChange)));
        return numerator.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_EVEN);
    }

    private BalanceTracker rolledOverTo(YearMonth targetMonth) {
        if (targetMonth.equals(yearMonth)) {
            return this;
        }
        return new BalanceTracker(targetMonth, BigDecimal.ZERO, lastBalance, targetMonth.atDay(1));
    }
}
