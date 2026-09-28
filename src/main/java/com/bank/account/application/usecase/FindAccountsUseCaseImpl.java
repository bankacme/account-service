package com.bank.account.application.usecase;

import com.bank.account.application.port.in.AccountFilter;
import com.bank.account.application.port.in.FindAccountsUseCase;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.domain.model.Account;
import io.reactivex.rxjava3.core.Flowable;

public class FindAccountsUseCaseImpl implements FindAccountsUseCase {

    private final AccountRepositoryPort repositoryPort;

    public FindAccountsUseCaseImpl(AccountRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Flowable<Account> execute(AccountFilter filter) {
        return repositoryPort.findAll(filter);
    }
}
