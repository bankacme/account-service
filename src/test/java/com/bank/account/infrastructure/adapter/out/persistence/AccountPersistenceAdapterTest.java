package com.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.account.application.port.in.AccountFilter;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountStatus;
import com.bank.account.domain.model.AccountType;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AccountPersistenceAdapterTest {

    @Autowired
    private AccountPersistenceAdapter adapter;

    @Autowired
    private AccountMongoRepository repository;

    @BeforeEach
    @AfterEach
    void cleanCollection() {
        repository.deleteAll().blockingAwait();
    }

    @Test
    void savingASecondActiveSavingsForTheSamePersonalCustomerTranslatesToSavingsLimitReached() {
        String customerId = "cust-" + UUID.randomUUID();
        adapter.save(AccountFixtures.openPersonalSavings(customerId, "60000000000001", "100.00")).blockingGet();

        Account second = AccountFixtures.openPersonalSavings(customerId, "60000000000002", "100.00");

        assertThatThrownBy(() -> adapter.save(second).blockingGet())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(error -> assertThat(error instanceof BusinessRuleViolationException violation
                        && "SAVINGS_LIMIT_REACHED".equals(violation.getErrorCode())).isTrue());
    }

    @Test
    void savingASecondActiveCheckingForTheSamePersonalCustomerTranslatesToCheckingLimitReached() {
        String customerId = "cust-" + UUID.randomUUID();
        adapter.save(AccountFixtures.openPersonalChecking(customerId, "60000000000003", "100.00")).blockingGet();

        Account second = AccountFixtures.openPersonalChecking(customerId, "60000000000004", "100.00");

        assertThatThrownBy(() -> adapter.save(second).blockingGet())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(error -> assertThat(error instanceof BusinessRuleViolationException violation
                        && "CHECKING_LIMIT_REACHED".equals(violation.getErrorCode())).isTrue());
    }

    @Test
    void aCollidingAccountNumberIsRegeneratedAndRetriedRatherThanFailing() {
        String takenNumber = "70000000000001";
        adapter.save(AccountFixtures.openPersonalSavings("cust-a", takenNumber, "100.00")).blockingGet();

        Account collidingSecond = AccountFixtures.openPersonalChecking("cust-b", takenNumber, "50.00");

        Account saved = adapter.save(collidingSecond).blockingGet();

        assertThat(saved.accountNumber().value()).isNotEqualTo(takenNumber);
        assertThat(saved.customerId()).isEqualTo("cust-b");
        assertThat(repository.findById(saved.id().value()).blockingGet().getAccountNumber())
                .isEqualTo(saved.accountNumber().value());
    }

    @Test
    void findAllFiltersByCustomerIdInTheQueryAndByTypeAndStatusInMemory() {
        String customerId = "cust-" + UUID.randomUUID();
        adapter.save(AccountFixtures.openPersonalSavings(customerId, "80000000000001", "100.00")).blockingGet();
        adapter.save(AccountFixtures.openPersonalChecking(customerId, "80000000000002", "100.00")).blockingGet();
        adapter.save(AccountFixtures.openPersonalSavings("other-customer", "80000000000003", "100.00"))
                .blockingGet();

        AccountFilter savingsForCustomer = new AccountFilter(customerId, AccountType.SAVINGS, AccountStatus.ACTIVE);

        assertThat(adapter.findAll(savingsForCustomer).toList().blockingGet()).hasSize(1);
        assertThat(adapter.findAll(new AccountFilter(customerId, null, null)).toList().blockingGet()).hasSize(2);
    }

    @Test
    void countActiveByCustomerAndTypeOnlyCountsActiveAccountsOfThatType() {
        String customerId = "cust-" + UUID.randomUUID();
        adapter.save(AccountFixtures.openPersonalSavings(customerId, "90000000000001", "100.00")).blockingGet();

        assertThat(adapter.countActiveByCustomerAndType(customerId, AccountType.SAVINGS).blockingGet())
                .isEqualTo(1L);
        assertThat(adapter.countActiveByCustomerAndType(customerId, AccountType.CHECKING).blockingGet())
                .isEqualTo(0L);
    }
}
