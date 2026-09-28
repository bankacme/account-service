package com.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.MovementType;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MongoUnitOfWorkAdapterTest {

    @Autowired
    private MongoUnitOfWorkAdapter unitOfWork;

    @Autowired
    private AccountMongoRepository accountRepository;

    @Autowired
    private AccountOperationMongoRepository operationRepository;

    @BeforeEach
    @AfterEach
    void cleanCollections() {
        accountRepository.deleteAll().blockingAwait();
        operationRepository.deleteAll().blockingAwait();
    }

    @Test
    void bothWritesCommitTogetherOnSuccess() {
        Account account = AccountFixtures.openPersonalSavings("cust-tx-ok-" + UUID.randomUUID(),
                "99999999999901", "1000.00");
        AccountOperation operation = AccountFixtures.appliedOperation(account.id(), MovementType.WITHDRAWAL,
                "100.00", "0.00", "900.00", 1);

        Account result = unitOfWork.saveAccountAndOperation(account, operation).blockingGet();

        assertThat(result.id()).isEqualTo(account.id());
        assertThat(accountRepository.existsById(account.id().value()).blockingGet()).isTrue();
        assertThat(operationRepository.existsById(operation.operationId()).blockingGet()).isTrue();
    }

    @Test
    void aFailureAfterTheFirstWriteRollsBothWritesBack() {
        Account account = AccountFixtures.openPersonalSavings("cust-tx-rollback-" + UUID.randomUUID(),
                "99999999999902", "1000.00");
        AccountId accountId = account.id();

        assertThatThrownBy(() -> unitOfWork.saveAccountForcingFailureAfterward(account).blockingGet())
                .isInstanceOf(IllegalStateException.class);

        assertThat(accountRepository.existsById(accountId.value()).blockingGet())
                .as("the account write must be rolled back when a later write in the same transaction never happens")
                .isFalse();
    }
}
