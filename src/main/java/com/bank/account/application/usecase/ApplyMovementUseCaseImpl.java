package com.bank.account.application.usecase;

import com.bank.account.application.command.ApplyMovementCommand;
import com.bank.account.application.port.in.ApplyMovementUseCase;
import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.application.port.out.OperationLogPort;
import com.bank.account.application.port.out.UnitOfWorkPort;
import com.bank.account.domain.event.MovementApplied;
import com.bank.account.domain.event.MovementRejected;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.MovementResult;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * data-model.md 2.3/2.4/3.1. Idempotent by {@code operationId}: a repeat with the same
 * account/type/amount replays the stored outcome without touching money again; a repeat
 * with different data is 409 OPERATION_ID_REUSED. A business-rule rejection only writes
 * {@code account_operations} (no transaction needed — the balance never moves); a
 * successful application writes {@code accounts} and {@code account_operations} together
 * atomically via {@link UnitOfWorkPort#saveAccountAndOperation}.
 */
public class ApplyMovementUseCaseImpl implements ApplyMovementUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final OperationLogPort operationLogPort;
    private final UnitOfWorkPort unitOfWorkPort;
    private final AccountEventPublisherPort eventPublisherPort;
    private final Clock clock;

    public ApplyMovementUseCaseImpl(AccountRepositoryPort accountRepositoryPort, OperationLogPort operationLogPort,
                                     UnitOfWorkPort unitOfWorkPort, AccountEventPublisherPort eventPublisherPort,
                                     Clock clock) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.operationLogPort = operationLogPort;
        this.unitOfWorkPort = unitOfWorkPort;
        this.eventPublisherPort = eventPublisherPort;
        this.clock = clock;
    }

    @Override
    public Single<MovementResult> execute(ApplyMovementCommand command) {
        return operationLogPort.find(command.operationId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(existing -> existing.isPresent() ? replay(existing.get(), command) : applyNew(command));
    }

    private Single<MovementResult> replay(AccountOperation existing, ApplyMovementCommand command) {
        if (!existing.matches(command.accountId(), command.type(), command.amount())) {
            return Single.error(new BusinessRuleViolationException("OPERATION_ID_REUSED",
                    "operationId " + command.operationId() + " was already used for a different account/type/amount"));
        }
        return switch (existing.status()) {
            case APPLIED, REVERSED -> Single.just(existing.toMovementResult());
            case REJECTED -> Single.error(new BusinessRuleViolationException(existing.reasonCode(),
                    "Operation " + command.operationId() + " was rejected: " + existing.reasonCode()));
        };
    }

    private Single<MovementResult> applyNew(ApplyMovementCommand command) {
        LocalDate date = command.date() != null ? command.date() : LocalDate.now(clock);
        return accountRepositoryPort.findById(command.accountId())
                .switchIfEmpty(Single.error(new AccountNotFoundException(command.accountId().value())))
                .flatMap(account -> tryApply(account, command, date))
                .flatMap(result -> eventPublisherPort.publish(MovementApplied.from(result))
                        .andThen(Single.just(result)));
    }

    private Single<MovementResult> tryApply(Account account, ApplyMovementCommand command, LocalDate date) {
        return Single.fromCallable(() -> account.applyMovement(command.operationId(), command.type(),
                        command.amount(), date, clock))
                .flatMap(outcome -> unitOfWorkPort.saveAccountAndOperation(outcome.account(),
                                AccountOperation.applied(outcome.result(), date, clock.instant()))
                        .map(saved -> outcome.result()))
                .onErrorResumeNext(error -> recordRejectionAndRethrow(command, date, error));
    }

    private Single<MovementResult> recordRejectionAndRethrow(ApplyMovementCommand command, LocalDate date,
                                                              Throwable error) {
        if (!(error instanceof BusinessRuleViolationException businessError)) {
            return Single.error(error);
        }
        AccountOperation rejection = AccountOperation.rejected(command.operationId(), command.accountId(),
                command.type(), command.amount(), date, businessError.getErrorCode(), clock.instant());
        return operationLogPort.save(rejection)
                .flatMapCompletable(saved -> eventPublisherPort.publish(new MovementRejected(command.operationId(),
                        command.accountId().value(), businessError.getErrorCode())))
                .andThen(Single.error(error));
    }
}
