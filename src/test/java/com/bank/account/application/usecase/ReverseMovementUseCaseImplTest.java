package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.application.usecase.TestAdapters.PassthroughUnitOfWorkPort;
import com.bank.account.application.usecase.TestAdapters.RecordingEventPublisherPort;
import com.bank.account.domain.exception.OperationNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountNumber;
import com.bank.account.domain.model.AccountStatus;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MovementResult;
import com.bank.account.domain.model.MovementType;
import com.bank.account.domain.model.ReversalResult;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReverseMovementUseCaseImplTest {

    private final LocalDate today = LocalDate.of(2026, 9, 27);
    private final Clock clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private final InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
    private final InMemoryOperationLog operationLog = new InMemoryOperationLog();
    private final RecordingEventPublisherPort eventPublisherPort = new RecordingEventPublisherPort();
    private final ApplyMovementUseCaseImpl applyUseCase = new ApplyMovementUseCaseImpl(accountRepository,
            operationLog, new PassthroughUnitOfWorkPort(accountRepository, operationLog), eventPublisherPort, clock);
    private final ReverseMovementUseCaseImpl reverseUseCase = new ReverseMovementUseCaseImpl(accountRepository,
            operationLog, new PassthroughUnitOfWorkPort(accountRepository, operationLog), eventPublisherPort, clock);

    private final AccountConditions savingsStandard = new AccountConditions(
            Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")), null, false);

    private Account openAccount(BigDecimal openingBalance) {
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(openingBalance), savingsStandard, List.of(), List.of(), null,
                new AccountNumber("00100000000001"), clock);
        return accountRepository.save(account).blockingGet();
    }

    @Test
    void reversesAWithdrawalEvenOnAnAccountClosedAfterward() {
        Account account = openAccount(new BigDecimal("100.00"));
        applyUseCase.execute(new com.bank.account.application.command.ApplyMovementCommand(
                account.id(), "op-1", MovementType.WITHDRAWAL, Money.of(new BigDecimal("40.00")), today))
                .blockingGet();
        // Close the account (balance is 60.00 after the withdrawal, so it must go to 0 first).
        applyUseCase.execute(new com.bank.account.application.command.ApplyMovementCommand(
                account.id(), "op-2", MovementType.WITHDRAWAL, Money.of(new BigDecimal("60.00")), today))
                .blockingGet();
        Account closed = accountRepository.findById(account.id()).blockingGet().close(clock);
        accountRepository.save(closed).blockingGet();
        assertThat(closed.status()).isEqualTo(AccountStatus.INACTIVE);

        TestObserver<ReversalResult> observer = reverseUseCase.execute(account.id(), "op-1").test();

        observer.assertComplete();
        assertThat(observer.values().get(0).newBalance().amount()).isEqualByComparingTo("40.00");
        assertThat(accountRepository.findById(account.id()).blockingGet().status())
                .isEqualTo(AccountStatus.INACTIVE); // reversal does not reactivate the account
    }

    @Test
    void repeatingTheReversalReplaysTheSameResultWithoutMovingMoneyTwice() {
        Account account = openAccount(new BigDecimal("100.00"));
        applyUseCase.execute(new com.bank.account.application.command.ApplyMovementCommand(
                account.id(), "op-1", MovementType.DEPOSIT, Money.of(new BigDecimal("20.00")), today)).blockingGet();

        ReversalResult first = reverseUseCase.execute(account.id(), "op-1").blockingGet();
        ReversalResult second = reverseUseCase.execute(account.id(), "op-1").blockingGet();

        assertThat(second).isEqualTo(first);
        assertThat(accountRepository.findById(account.id()).blockingGet().balance().amount())
                .isEqualByComparingTo("100.00"); // not restored twice
    }

    @Test
    void rejectsReversingAnUnknownOperation() {
        Account account = openAccount(new BigDecimal("100.00"));

        reverseUseCase.execute(account.id(), "never-existed").test()
                .assertError(OperationNotFoundException.class);
    }

    @Test
    void applyMovementReplaysTheOriginalResultEvenAfterItWasReversed() {
        // data-model.md 2.3: "REVERSED -> el MovementResult original" for ApplyMovementUseCase's
        // own idempotency, even though the operation has since moved on to REVERSED.
        Account account = openAccount(new BigDecimal("100.00"));
        com.bank.account.application.command.ApplyMovementCommand command =
                new com.bank.account.application.command.ApplyMovementCommand(account.id(), "op-1",
                        MovementType.WITHDRAWAL, Money.of(new BigDecimal("30.00")), today);
        MovementResult originalResult = applyUseCase.execute(command).blockingGet();
        reverseUseCase.execute(account.id(), "op-1").blockingGet();

        MovementResult replay = applyUseCase.execute(command).blockingGet();

        assertThat(replay).isEqualTo(originalResult);
    }
}
