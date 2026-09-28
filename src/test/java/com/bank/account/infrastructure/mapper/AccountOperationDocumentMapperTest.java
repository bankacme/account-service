package com.bank.account.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MovementType;
import com.bank.account.infrastructure.adapter.out.persistence.AccountOperationDocument;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AccountOperationDocumentMapperTest {

    private final AccountOperationDocumentMapper mapper = new AccountOperationDocumentMapper();
    private final AccountId accountId = AccountId.newId();

    @Test
    void roundTripsAnAppliedOperation() {
        AccountOperation operation = AccountFixtures.appliedOperation(accountId, MovementType.DEPOSIT, "100.00",
                "0.00", "1100.00", 1);

        AccountOperationDocument document = mapper.toDocument(operation);

        assertThat(document.getId()).isEqualTo(operation.operationId());
        assertThat(document.getStatus()).isEqualTo("APPLIED");
        assertThat(document.getFee()).isEqualByComparingTo("0.00");
        assertThat(document.getReasonCode()).isNull();
        assertThat(document.getReversedBalance()).isNull();
        assertThat(mapper.toDomain(document)).isEqualTo(operation);
    }

    @Test
    void roundTripsARejectedOperationWithNoAmountFields() {
        AccountOperation operation = AccountOperation.rejected("op-1", accountId, MovementType.WITHDRAWAL,
                Money.of("50.00"), LocalDate.now(AccountFixtures.CLOCK), "INSUFFICIENT_FUNDS",
                AccountFixtures.CLOCK.instant());

        AccountOperationDocument document = mapper.toDocument(operation);

        assertThat(document.getStatus()).isEqualTo("REJECTED");
        assertThat(document.getReasonCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(document.getFee()).isNull();
        assertThat(document.getNewBalance()).isNull();
        assertThat(document.getMovementNumber()).isNull();
        assertThat(mapper.toDomain(document)).isEqualTo(operation);
    }

    @Test
    void roundTripsAReversedOperationWithItsReversedBalance() {
        AccountOperation applied = AccountFixtures.appliedOperation(accountId, MovementType.WITHDRAWAL, "50.00",
                "2.00", "948.00", 6);
        AccountOperation reversed = applied.reversed(Money.of("1000.00"), AccountFixtures.CLOCK.instant());

        AccountOperationDocument document = mapper.toDocument(reversed);

        assertThat(document.getStatus()).isEqualTo("REVERSED");
        assertThat(document.getReversedBalance()).isEqualByComparingTo("1000.00");
        // The original fee/newBalance/movementNumber survive the reversal (data-model.md
        // 2.3: an idempotent replay of ApplyMovementUseCase still needs them).
        assertThat(document.getFee()).isEqualByComparingTo("2.00");
        assertThat(document.getNewBalance()).isEqualByComparingTo("948.00");
        assertThat(mapper.toDomain(document)).isEqualTo(reversed);
    }
}
