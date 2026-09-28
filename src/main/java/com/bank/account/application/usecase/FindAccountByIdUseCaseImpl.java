package com.bank.account.application.usecase;

import com.bank.account.application.port.in.FindAccountByIdUseCase;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import io.reactivex.rxjava3.core.Single;

public class FindAccountByIdUseCaseImpl implements FindAccountByIdUseCase {

    private final AccountRepositoryPort repositoryPort;

    public FindAccountByIdUseCaseImpl(AccountRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Single<Account> execute(AccountId id) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Single.error(new AccountNotFoundException(id.value())));
    }
}
