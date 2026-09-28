package com.bank.account.application.usecase;

import com.bank.account.application.port.in.ReverseMovementUseCase;
import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.application.port.out.OperationLogPort;
import com.bank.account.application.port.out.UnitOfWorkPort;
import com.bank.account.domain.event.MovementReversed;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.exception.OperationNotFoundException;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.ReversalResult;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;
import java.time.LocalDate;

/**
 * data-model.md 3.2. Unlike ApplyMovementUseCase, a failed reversal (INSUFFICIENT_FUNDS)
 * is NOT recorded as its own account_operations entry — there's nothing to make
 * idempotent about a reversal that never happened, and the ficha only documents that
 * shape for rejected new movements (2.3). Applies even to an INACTIVE account: nothing
 * here calls Account.requireActive (Account.reverseMovement doesn't either, R2).
 */
public class ReverseMovementUseCaseImpl implements ReverseMovementUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final OperationLogPort operationLogPort;
    private final UnitOfWorkPort unitOfWorkPort;
    private final AccountEventPublisherPort eventPublisherPort;
    private final Clock clock;

    public ReverseMovementUseCaseImpl(AccountRepositoryPort accountRepositoryPort, OperationLogPort operationLogPort,
                                       UnitOfWorkPort unitOfWorkPort, AccountEventPublisherPort eventPublisherPort,
                                       Clock clock) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.operationLogPort = operationLogPort;
        this.unitOfWorkPort = unitOfWorkPort;
        this.eventPublisherPort = eventPublisherPort;
        this.clock = clock;
    }

    @Override
    public Single<ReversalResult> execute(AccountId accountId, String operationId) {
        return operationLogPort.find(operationId)
                .switchIfEmpty(Single.error(new OperationNotFoundException(operationId, accountId.value())))
                .flatMap(operation -> switch (operation.status()) {
                    case REVERSED -> Single.just(operation.toReversalResult());
                    case REJECTED -> Single.error(new BusinessRuleViolationException("OPERATION_NOT_APPLIED",
                            "Operation " + operationId + " was rejected; there is nothing to reverse"));
                    case APPLIED -> reverseApplied(accountId, operation);
                });
    }

    private Single<ReversalResult> reverseApplied(AccountId accountId, AccountOperation operation) {
        LocalDate today = LocalDate.now(clock);
        return accountRepositoryPort.findById(accountId)
                .switchIfEmpty(Single.error(new AccountNotFoundException(accountId.value())))
                .flatMap(account -> Single.fromCallable(() -> account.reverseMovement(operation.operationId(),
                        operation.type(), operation.amount(), operation.fee(), operation.yearMonth(), today, clock)))
                .flatMap(outcome -> unitOfWorkPort.saveAccountAndOperation(outcome.account(),
                                operation.reversed(outcome.result().newBalance(), clock.instant()))
                        .map(saved -> outcome.result()))
                .flatMap(result -> eventPublisherPort.publish(MovementReversed.from(result))
                        .andThen(Single.just(result)));
    }
}
