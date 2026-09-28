package com.bank.account.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.DocumentType;
import com.bank.account.infrastructure.adapter.out.persistence.AccountDocument;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountDocumentMapperTest {

    private final AccountDocumentMapper mapper = new AccountDocumentMapper(new AccountConditionsMapper());

    @Test
    void roundTripsAPersonalSavingsAccountWithNoParties() {
        Account account = AccountFixtures.openPersonalSavings("cust-1", "12345678901234", "1000.00");

        AccountDocument document = mapper.toDocument(account);

        assertThat(document.getId()).isEqualTo(account.id().value());
        assertThat(document.getAccountNumber()).isEqualTo("12345678901234");
        assertThat(document.getCurrency()).isEqualTo("PEN");
        assertThat(document.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(document.getHolders()).isEmpty();
        assertThat(document.getSigners()).isEmpty();
        assertThat(document.getMovementDayOfMonth()).isNull();
        assertThat(mapper.toDomain(document)).isEqualTo(account);
    }

    @Test
    void roundTripsABusinessCheckingAccountWithAHolder() {
        Account account = AccountFixtures.openBusinessChecking("cust-2", "22345678901234", "500.00");

        AccountDocument document = mapper.toDocument(account);

        assertThat(document.getHolders()).hasSize(1);
        assertThat(document.getHolders().get(0).getDocument().getType()).isEqualTo("RUC");
        assertThat(document.getHolders().get(0).getDocument().getNumber()).isEqualTo("20512345678");
        assertThat(document.getHolders().get(0).getFullName()).isEqualTo("Bodega San Martin SAC");
        assertThat(mapper.toDomain(document)).isEqualTo(account);
    }

    @Test
    void roundTripsAFixedTermAccountsMovementDay() {
        Account account = AccountFixtures.openFixedTerm("cust-3", "32345678901234", "200.00", 15);

        AccountDocument document = mapper.toDocument(account);

        assertThat(document.getMovementDayOfMonth()).isEqualTo(15);
        assertThat(mapper.toDomain(document)).isEqualTo(account);
    }

    @Test
    void roundTripsMonthlyActivityAndBalanceTracker() {
        Account account = AccountFixtures.openPersonalChecking("cust-4", "42345678901234", "300.00");

        AccountDocument document = mapper.toDocument(account);

        assertThat(document.getMonthlyActivity().getYearMonth())
                .isEqualTo(account.monthlyActivity().yearMonth());
        assertThat(document.getBalanceTracker().getLastChangeDate())
                .isEqualTo(account.balanceTracker().lastChangeDate());
        assertThat(document.getBalanceTracker().getLastBalance()).isEqualByComparingTo("300.00");
        assertThat(mapper.toDomain(document)).isEqualTo(account);
    }

    @Test
    void mapsSignersSeparatelyFromHolders() {
        Account account = AccountFixtures.openBusinessChecking("cust-5", "52345678901234", "500.00");
        AccountParty signer = new AccountParty(DocumentType.DNI, "87654321", "Firmante Autorizado");
        Account withSigner = account.updateParties(account.alias(), account.holders(), List.of(signer),
                AccountFixtures.CLOCK);

        AccountDocument document = mapper.toDocument(withSigner);

        assertThat(document.getSigners()).hasSize(1);
        assertThat(document.getSigners().get(0).getFullName()).isEqualTo("Firmante Autorizado");
        assertThat(document.getHolders()).hasSize(1);
        assertThat(mapper.toDomain(document)).isEqualTo(withSigner);
    }
}
