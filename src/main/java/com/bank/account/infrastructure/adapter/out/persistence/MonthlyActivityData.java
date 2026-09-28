package com.bank.account.infrastructure.adapter.out.persistence;

import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyActivityData {

    private YearMonth yearMonth;

    private int movementCount;
}
