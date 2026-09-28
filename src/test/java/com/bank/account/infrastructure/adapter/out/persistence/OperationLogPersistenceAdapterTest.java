package com.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.MovementType;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OperationLogPersistenceAdapterTest {

    @Autowired
    private OperationLogPersistenceAdapter adapter;

    @Autowired
    private AccountOperationMongoRepository repository;

    @BeforeEach
    @AfterEach
    void cleanCollection() {
        repository.deleteAll().blockingAwait();
    }

    @Test
    void aSavedOperationIsFoundAgainByItsOperationId() {
        AccountId accountId = AccountId.newId();
        AccountOperation operation = AccountFixtures.appliedOperation(accountId, MovementType.DEPOSIT, "100.00",
                "0.00", "1100.00", 1);

        adapter.save(operation).blockingGet();

        assertThat(adapter.find(operation.operationId()).blockingGet()).isEqualTo(operation);
    }

    @Test
    void findReturnsEmptyForAnUnknownOperationId() {
        assertThat(adapter.find("unknown-operation-id").blockingGet()).isNull();
    }
}
