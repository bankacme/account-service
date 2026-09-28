package com.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

@SpringBootTest
class AccountMongoRepositoryTest {

    @Autowired
    private AccountMongoRepository repository;

    @BeforeEach
    @AfterEach
    void cleanCollection() {
        repository.deleteAll().blockingAwait();
    }

    @Test
    void findByCustomerIdAndCountByCustomerIdAndTypeAndStatusAgreeWithWhatWasSaved() {
        String customerId = "cust-" + UUID.randomUUID();
        repository.save(minimalAccount("acc-1", "10000000000001", customerId, "PERSONAL", "SAVINGS", "ACTIVE"))
                .blockingGet();
        repository.save(minimalAccount("acc-2", "10000000000002", customerId, "PERSONAL", "CHECKING", "ACTIVE"))
                .blockingGet();
        repository.save(minimalAccount("acc-3", "10000000000003", "other-customer", "PERSONAL", "SAVINGS", "ACTIVE"))
                .blockingGet();

        assertThat(repository.findByCustomerId(customerId).toList().blockingGet()).hasSize(2);
        assertThat(repository.countByCustomerIdAndTypeAndStatus(customerId, "SAVINGS", "ACTIVE").blockingGet())
                .isEqualTo(1L);
        assertThat(repository.countByCustomerIdAndTypeAndStatus(customerId, "SAVINGS", "INACTIVE").blockingGet())
                .isEqualTo(0L);
    }

    @Test
    void theUniqueIndexRejectsADuplicateAccountNumber() {
        repository.save(minimalAccount("acc-4", "20000000000001", "cust-a", "PERSONAL", "SAVINGS", "ACTIVE"))
                .blockingGet();

        assertThatThrownBy(() -> repository
                .save(minimalAccount("acc-5", "20000000000001", "cust-b", "PERSONAL", "CHECKING", "ACTIVE"))
                .blockingGet())
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_account_number");
    }

    @Test
    void thePartialIndexRejectsASecondActivePersonalSavingsForTheSameCustomer() {
        String customerId = "cust-" + UUID.randomUUID();
        repository.save(minimalAccount("acc-6", "30000000000001", customerId, "PERSONAL", "SAVINGS", "ACTIVE"))
                .blockingGet();

        assertThatThrownBy(() -> repository
                .save(minimalAccount("acc-7", "30000000000002", customerId, "PERSONAL", "SAVINGS", "ACTIVE"))
                .blockingGet())
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_personal_savings");
    }

    @Test
    void thePartialIndexRejectsASecondActivePersonalCheckingForTheSameCustomer() {
        String customerId = "cust-" + UUID.randomUUID();
        repository.save(minimalAccount("acc-8", "40000000000001", customerId, "PERSONAL", "CHECKING", "ACTIVE"))
                .blockingGet();

        assertThatThrownBy(() -> repository
                .save(minimalAccount("acc-9", "40000000000002", customerId, "PERSONAL", "CHECKING", "ACTIVE"))
                .blockingGet())
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_personal_checking");
    }

    @Test
    void thePartialFilterDoesNotBlockASecondInactiveOrBusinessSavingsAccount() {
        String customerId = "cust-" + UUID.randomUUID();
        repository.save(minimalAccount("acc-10", "50000000000001", customerId, "PERSONAL", "SAVINGS", "ACTIVE"))
                .blockingGet();

        repository.save(minimalAccount("acc-11", "50000000000002", customerId, "PERSONAL", "SAVINGS", "INACTIVE"))
                .blockingGet();

        String businessCustomerId = "cust-" + UUID.randomUUID();
        repository.save(minimalAccount("acc-12", "50000000000003", businessCustomerId, "BUSINESS", "SAVINGS",
                "ACTIVE")).blockingGet();
        repository.save(minimalAccount("acc-13", "50000000000004", businessCustomerId, "BUSINESS", "SAVINGS",
                "ACTIVE")).blockingGet();

        assertThat(repository.findByCustomerId(customerId).toList().blockingGet()).hasSize(2);
        assertThat(repository.findByCustomerId(businessCustomerId).toList().blockingGet()).hasSize(2);
    }

    private AccountDocument minimalAccount(String id, String accountNumber, String customerId, String customerType,
                                            String type, String status) {
        Instant now = Instant.parse("2026-09-27T10:00:00Z");
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        YearMonth thisMonth = YearMonth.from(today);
        return AccountDocument.builder()
                .id(id)
                .accountNumber(accountNumber)
                .customerId(customerId)
                .customerType(customerType)
                .customerProfile("STANDARD")
                .type(type)
                .balance(BigDecimal.ZERO)
                .currency("PEN")
                .conditions(AccountConditionsData.builder()
                        .maintenanceFee(BigDecimal.ZERO)
                        .minimumOpeningAmount(BigDecimal.ZERO)
                        .freeTransactionsLimit(5)
                        .transactionFee(BigDecimal.valueOf(2))
                        .requiresCreditCard(false)
                        .build())
                .monthlyActivity(MonthlyActivityData.builder().yearMonth(thisMonth).movementCount(0).build())
                .balanceTracker(BalanceTrackerData.builder()
                        .yearMonth(thisMonth)
                        .accumulatedBalanceDays(BigDecimal.ZERO)
                        .lastBalance(BigDecimal.ZERO)
                        .lastChangeDate(today)
                        .build())
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
