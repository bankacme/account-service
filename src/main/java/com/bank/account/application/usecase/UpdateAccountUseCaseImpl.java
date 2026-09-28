package com.bank.account.application.usecase;

import com.bank.account.application.command.UpdateAccountCommand;
import com.bank.account.application.port.in.UpdateAccountUseCase;
import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.domain.event.AccountUpdated;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.model.Account;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;

public class UpdateAccountUseCaseImpl implements UpdateAccountUseCase {

    private final AccountRepositoryPort repositoryPort;
    private final AccountEventPublisherPort eventPublisherPort;
    private final Clock clock;

    public UpdateAccountUseCaseImpl(AccountRepositoryPort repositoryPort,
                                     AccountEventPublisherPort eventPublisherPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.clock = clock;
    }

    @Override
    public Single<Account> execute(UpdateAccountCommand command) {
        return repositoryPort.findById(command.accountId())
                .switchIfEmpty(Single.error(new AccountNotFoundException(command.accountId().value())))
                .flatMap(account -> Single.fromCallable(() -> account.updateParties(command.alias(),
                        command.holders(), command.signers(), clock)))
                .flatMap(repositoryPort::save)
                .flatMap(saved -> eventPublisherPort.publish(AccountUpdated.from(saved))
                        .andThen(Single.just(saved)));
    }
}
