package com.bank.account.domain.model;

import java.time.Clock;
import java.time.YearMonth;

public record MonthlyActivity(YearMonth yearMonth, int movementCount) {

    public MonthlyActivity {
        if (yearMonth == null) {
            throw new IllegalArgumentException("yearMonth must not be null");
        }
        if (movementCount < 0) {
            throw new IllegalArgumentException("movementCount must not be negative");
        }
    }

    public static MonthlyActivity initial(Clock clock) {
        return new MonthlyActivity(YearMonth.now(clock), 0);
    }

    public MonthlyActivity forMonth(YearMonth month) {
        return month.equals(yearMonth) ? this : new MonthlyActivity(month, 0);
    }

    public MonthlyActivity withCount(int newCount) {
        return new MonthlyActivity(yearMonth, newCount);
    }
}
