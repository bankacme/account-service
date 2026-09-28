package com.bank.account.application.usecase;

import com.bank.account.application.port.in.CloseAccountUseCase;
import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.domain.event.AccountClosed;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;

public class CloseAccountUseCaseImpl implements CloseAccountUseCase {

    private final AccountRepositoryPort repositoryPort;
    private final AccountEventPublisherPort eventPublisherPort;
    private final Clock clock;

    public CloseAccountUseCaseImpl(AccountRepositoryPort repositoryPort,
                                    AccountEventPublisherPort eventPublisherPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.clock = clock;
    }

    @Override
    public Completable execute(AccountId id) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Single.error(new AccountNotFoundException(id.value())))
                .flatMap(this::closeIfActive)
                .flatMapCompletable(result -> result.wasActive()
                        ? eventPublisherPort.publish(AccountClosed.from(result.account()))
                        : Completable.complete());
    }

    private Single<CloseResult> closeIfActive(Account account) {
        boolean wasActive = account.status() == AccountStatus.ACTIVE;
        Account closed = account.close(clock);
        return wasActive
                ? repositoryPort.save(closed).map(saved -> new CloseResult(saved, true))
                : Single.just(new CloseResult(closed, false));
    }

    private record CloseResult(Account account, boolean wasActive) {
    }
}
