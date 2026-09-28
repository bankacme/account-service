package com.bank.account.domain.model;

import java.time.YearMonth;

public record DailyAverage(YearMonth yearMonth, Money average, Money minimum, boolean meetsMinimum) {

    public DailyAverage {
        if (yearMonth == null || average == null || minimum == null) {
            throw new IllegalArgumentException("yearMonth, average and minimum are required");
        }
    }

    public static DailyAverage of(YearMonth yearMonth, Money average, Money minimum) {
        return new DailyAverage(yearMonth, average, minimum, average.isGreaterThanOrEqualTo(minimum));
    }
}
