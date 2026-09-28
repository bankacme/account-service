package com.bank.account.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.domain.model.AccountConditions;
import com.bank.account.infrastructure.adapter.out.persistence.AccountConditionsData;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import org.junit.jupiter.api.Test;

class AccountConditionsMapperTest {

    private final AccountConditionsMapper mapper = new AccountConditionsMapper();

    @Test
    void roundTripsWithoutAMinimumDailyAverage() {
        AccountConditions conditions = AccountFixtures.standardConditions();

        AccountConditionsData data = mapper.toData(conditions);

        assertThat(data.getMinimumDailyAverage()).isNull();
        assertThat(data.isRequiresCreditCard()).isFalse();
        assertThat(mapper.toDomain(data)).isEqualTo(conditions);
    }

    @Test
    void roundTripsWithAMinimumDailyAverageAndRequiredCard() {
        AccountConditions conditions = AccountFixtures.vipSavingsConditions();

        AccountConditionsData data = mapper.toData(conditions);

        assertThat(data.getMinimumDailyAverage()).isEqualByComparingTo("500.00");
        assertThat(data.isRequiresCreditCard()).isTrue();
        assertThat(mapper.toDomain(data)).isEqualTo(conditions);
    }
}
