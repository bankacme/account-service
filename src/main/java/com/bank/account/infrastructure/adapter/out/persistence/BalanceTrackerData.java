package com.bank.account.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceTrackerData {

    private YearMonth yearMonth;

    private BigDecimal accumulatedBalanceDays;

    private BigDecimal lastBalance;

    private LocalDate lastChangeDate;
}
