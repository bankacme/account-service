package com.bank.account.infrastructure.mapper;

import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.Money;
import com.bank.account.infrastructure.adapter.out.persistence.AccountConditionsData;
import org.springframework.stereotype.Component;

@Component
public class AccountConditionsMapper {

    public AccountConditionsData toData(AccountConditions conditions) {
        return AccountConditionsData.builder()
                .maintenanceFee(conditions.maintenanceFee().amount())
                .minimumOpeningAmount(conditions.minimumOpeningAmount().amount())
                .monthlyMovementLimit(conditions.monthlyMovementLimit())
                .freeTransactionsLimit(conditions.freeTransactionsLimit())
                .transactionFee(conditions.transactionFee().amount())
                .minimumDailyAverage(conditions.minimumDailyAverage() == null
                        ? null : conditions.minimumDailyAverage().amount())
                .requiresCreditCard(conditions.requiresCreditCard())
                .build();
    }

    public AccountConditions toDomain(AccountConditionsData data) {
        return new AccountConditions(
                Money.of(data.getMaintenanceFee()),
                Money.of(data.getMinimumOpeningAmount()),
                data.getMonthlyMovementLimit(),
                data.getFreeTransactionsLimit(),
                Money.of(data.getTransactionFee()),
                data.getMinimumDailyAverage() == null ? null : Money.of(data.getMinimumDailyAverage()),
                data.isRequiresCreditCard());
    }
}
