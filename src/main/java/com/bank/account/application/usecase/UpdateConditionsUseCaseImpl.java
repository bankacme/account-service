package com.bank.account.application.usecase;

import com.bank.account.application.command.UpdateConditionsCommand;
import com.bank.account.application.port.in.UpdateConditionsUseCase;
import com.bank.account.application.port.out.AccountProductRepositoryPort;
import com.bank.account.application.port.out.ProductCachePort;
import com.bank.account.domain.exception.ConditionsNotFoundException;
import com.bank.account.domain.model.AccountProduct;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;

public class UpdateConditionsUseCaseImpl implements UpdateConditionsUseCase {

    private final AccountProductRepositoryPort repositoryPort;
    private final ProductCachePort cachePort;
    private final Clock clock;

    public UpdateConditionsUseCaseImpl(AccountProductRepositoryPort repositoryPort, ProductCachePort cachePort,
                                        Clock clock) {
        this.repositoryPort = repositoryPort;
        this.cachePort = cachePort;
        this.clock = clock;
    }

    @Override
    public Single<AccountProduct> execute(UpdateConditionsCommand command) {
        return repositoryPort.findById(command.productId())
                .switchIfEmpty(Single.error(new ConditionsNotFoundException(command.productId().value())))
                .flatMap(product -> Single.fromCallable(
                        () -> product.withConditions(command.conditions(), clock)))
                .flatMap(repositoryPort::save)
                .flatMap(saved -> cachePort.evict(saved.id()).andThen(Single.just(saved)));
    }
}
