package com.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AccountOperationMongoRepositoryTest {

    @Autowired
    private AccountOperationMongoRepository repository;

    @BeforeEach
    @AfterEach
    void cleanCollection() {
        repository.deleteAll().blockingAwait();
    }

    @Test
    void findByIdReturnsWhatWasSavedUnderItsOperationId() {
        Instant now = Instant.parse("2026-09-27T10:00:00Z");
        AccountOperationDocument operation = AccountOperationDocument.builder()
                .id("op-1")
                .accountId("acc-1")
                .type("DEPOSIT")
                .amount(BigDecimal.valueOf(100))
                .date(LocalDate.of(2026, 9, 27))
                .yearMonth(YearMonth.of(2026, 9))
                .status("APPLIED")
                .fee(BigDecimal.ZERO)
                .newBalance(BigDecimal.valueOf(1100))
                .movementNumber(1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        repository.save(operation).blockingGet();

        AccountOperationDocument found = repository.findById("op-1").blockingGet();
        assertThat(found.getAccountId()).isEqualTo("acc-1");
        assertThat(found.getAmount()).isEqualByComparingTo("100");
        assertThat(found.getStatus()).isEqualTo("APPLIED");
    }
}
