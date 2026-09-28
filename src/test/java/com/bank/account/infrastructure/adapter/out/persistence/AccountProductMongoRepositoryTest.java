package com.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

@SpringBootTest
class AccountProductMongoRepositoryTest {

    @Autowired
    private AccountProductMongoRepository repository;

    @BeforeEach
    @AfterEach
    void cleanCollection() {
        repository.deleteAll().blockingAwait();
    }

    @Test
    void findByAccountTypeAndProfileAndTheSingleFieldQueriesAgreeWithWhatWasSaved() {
        repository.save(product("SAVINGS_STANDARD", "SAVINGS", "STANDARD")).blockingGet();
        repository.save(product("SAVINGS_VIP", "SAVINGS", "VIP")).blockingGet();
        repository.save(product("CHECKING_STANDARD", "CHECKING", "STANDARD")).blockingGet();

        assertThat(repository.findByAccountTypeAndProfile("SAVINGS", "VIP").blockingGet().getId())
                .isEqualTo("SAVINGS_VIP");
        assertThat(repository.findByAccountTypeAndProfile("SAVINGS", "PYME").blockingGet()).isNull();
        assertThat(repository.findByAccountType("SAVINGS").toList().blockingGet()).hasSize(2);
        assertThat(repository.findByProfile("STANDARD").toList().blockingGet()).hasSize(2);
    }

    @Test
    void theUniqueIndexRejectsADuplicateTypeProfilePair() {
        repository.save(product("SAVINGS_VIP", "SAVINGS", "VIP")).blockingGet();

        assertThatThrownBy(() -> repository.save(product("SAVINGS_VIP_DUPLICATE", "SAVINGS", "VIP")).blockingGet())
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_product_type_profile");
    }

    private AccountProductDocument product(String id, String accountType, String profile) {
        Instant now = Instant.parse("2026-09-27T10:00:00Z");
        return AccountProductDocument.builder()
                .id(id)
                .accountType(accountType)
                .profile(profile)
                .conditions(AccountConditionsData.builder()
                        .maintenanceFee(BigDecimal.ZERO)
                        .minimumOpeningAmount(BigDecimal.ZERO)
                        .freeTransactionsLimit(5)
                        .transactionFee(BigDecimal.valueOf(2))
                        .requiresCreditCard(false)
                        .build())
                .updatedAt(now)
                .build();
    }
}
