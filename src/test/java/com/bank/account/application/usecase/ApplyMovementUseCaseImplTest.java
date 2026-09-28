package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.application.command.ApplyMovementCommand;
import com.bank.account.application.usecase.TestAdapters.PassthroughUnitOfWorkPort;
import com.bank.account.application.usecase.TestAdapters.RecordingEventPublisherPort;
import com.bank.account.domain.event.MovementApplied;
import com.bank.account.domain.event.MovementRejected;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MovementResult;
import com.bank.account.domain.model.MovementType;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplyMovementUseCaseImplTest {

    private final LocalDate today = LocalDate.of(2026, 9, 27);
    private final Clock clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private final InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
    private final InMemoryOperationLog operationLog = new InMemoryOperationLog();
    private final RecordingEventPublisherPort eventPublisherPort = new RecordingEventPublisherPort();
    private final ApplyMovementUseCaseImpl useCase = new ApplyMovementUseCaseImpl(accountRepository, operationLog,
            new PassthroughUnitOfWorkPort(accountRepository, operationLog), eventPublisherPort, clock);

    private final AccountConditions savingsStandard = new AccountConditions(
            Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")), null, false);

    private Account openAccount(BigDecimal openingBalance) {
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(openingBalance), savingsStandard, List.of(), List.of(), null,
                new com.bank.account.domain.model.AccountNumber("00100000000001"), clock);
        return accountRepository.save(account).blockingGet();
    }

    @Test
    void appliesADepositAndPublishesMovementApplied() {
        Account account = openAccount(new BigDecimal("100.00"));
        ApplyMovementCommand command = new ApplyMovementCommand(account.id(), "op-1", MovementType.DEPOSIT,
                Money.of(new BigDecimal("50.00")), today);

        TestObserver<MovementResult> observer = useCase.execute(command).test();

        observer.assertComplete();
        assertThat(observer.values().get(0).newBalance().amount()).isEqualByComparingTo("150.00");
        assertThat(eventPublisherPort.published()).hasSize(1);
        assertThat(eventPublisherPort.published().get(0)).isInstanceOf(MovementApplied.class);
        assertThat(operationLog.size()).isEqualTo(1);
    }

    @Test
    void repeatingTheSameOperationIdReplaysTheResultWithoutApplyingTwice() {
        Account account = openAccount(new BigDecimal("100.00"));
        ApplyMovementCommand command = new ApplyMovementCommand(account.id(), "op-1", MovementType.WITHDRAWAL,
                Money.of(new BigDecimal("30.00")), today);

        MovementResult first = useCase.execute(command).blockingGet();
        MovementResult second = useCase.execute(command).blockingGet();

        assertThat(second).isEqualTo(first);
        assertThat(accountRepository.findById(account.id()).blockingGet().balance().amount())
                .isEqualByComparingTo("70.00"); // not 40.00: the second call must not withdraw again
        assertThat(eventPublisherPort.published()).hasSize(1); // not republished on replay
    }

    @Test
    void reusingAnOperationIdWithADifferentAmountIsRejected() {
        Account account = openAccount(new BigDecimal("100.00"));
        ApplyMovementCommand original = new ApplyMovementCommand(account.id(), "op-1", MovementType.WITHDRAWAL,
                Money.of(new BigDecimal("30.00")), today);
        ApplyMovementCommand reused = new ApplyMovementCommand(account.id(), "op-1", MovementType.WITHDRAWAL,
                Money.of(new BigDecimal("99.00")), today);
        useCase.execute(original).test().assertComplete();

        useCase.execute(reused).test()
                .assertError(error -> error instanceof BusinessRuleViolationException businessError
                        && "OPERATION_ID_REUSED".equals(businessError.getErrorCode()));
    }

    @Test
    void aRejectedMovementIsPersistedAndReplayedOnRetryWithoutChangingTheBalance() {
        Account account = openAccount(new BigDecimal("10.00"));
        ApplyMovementCommand command = new ApplyMovementCommand(account.id(), "op-1", MovementType.WITHDRAWAL,
                Money.of(new BigDecimal("1000.00")), today); // INSUFFICIENT_FUNDS

        useCase.execute(command).test().assertError(BusinessRuleViolationException.class);
        useCase.execute(command).test().assertError(BusinessRuleViolationException.class); // replay, same code

        assertThat(accountRepository.findById(account.id()).blockingGet().balance().amount())
                .isEqualByComparingTo("10.00");
        assertThat(eventPublisherPort.published()).hasSize(1); // MovementRejected published only once
        assertThat(eventPublisherPort.published().get(0)).isInstanceOf(MovementRejected.class);
    }
}
