package com.bank.account.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.infrastructure.adapter.out.persistence.AccountProductDocument;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import org.junit.jupiter.api.Test;

class AccountProductDocumentMapperTest {

    private final AccountProductDocumentMapper mapper = new AccountProductDocumentMapper(new AccountConditionsMapper());

    @Test
    void roundTripsAStandardCatalogEntry() {
        AccountProduct product = AccountProduct.create(AccountType.SAVINGS, CustomerProfile.STANDARD,
                AccountFixtures.standardConditions(), AccountFixtures.CLOCK);

        AccountProductDocument document = mapper.toDocument(product);

        assertThat(document.getId()).isEqualTo("SAVINGS_STANDARD");
        assertThat(document.getAccountType()).isEqualTo("SAVINGS");
        assertThat(document.getProfile()).isEqualTo("STANDARD");
        assertThat(mapper.toDomain(document)).isEqualTo(product);
    }

    @Test
    void roundTripsAVipCatalogEntryWithAMinimumAverage() {
        AccountProduct product = AccountProduct.create(AccountType.SAVINGS, CustomerProfile.VIP,
                AccountFixtures.vipSavingsConditions(), AccountFixtures.CLOCK);

        AccountProductDocument document = mapper.toDocument(product);

        assertThat(document.getId()).isEqualTo("SAVINGS_VIP");
        assertThat(document.getConditions().getMinimumDailyAverage()).isEqualByComparingTo("500.00");
        assertThat(mapper.toDomain(document)).isEqualTo(product);
    }
}
