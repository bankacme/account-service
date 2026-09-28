package com.bank.account.application.usecase;

import com.bank.account.application.port.in.FindConditionsUseCase;
import com.bank.account.application.port.in.ProductFilter;
import com.bank.account.application.port.out.AccountProductRepositoryPort;
import com.bank.account.domain.model.AccountProduct;
import io.reactivex.rxjava3.core.Flowable;

public class FindConditionsUseCaseImpl implements FindConditionsUseCase {

    private final AccountProductRepositoryPort repositoryPort;

    public FindConditionsUseCaseImpl(AccountProductRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Flowable<AccountProduct> execute(ProductFilter filter) {
        return repositoryPort.findAll(filter);
    }
}
