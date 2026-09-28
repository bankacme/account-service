package com.bank.account.infrastructure.adapter.out.noop;

import com.bank.account.application.port.out.ProductCachePort;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.ProductId;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.stereotype.Component;

@Component
public class NoOpProductCacheAdapter implements ProductCachePort {

    @Override
    public Maybe<AccountProduct> get(ProductId id) {
        return Maybe.empty();
    }

    @Override
    public Completable put(AccountProduct product) {
        return Completable.complete();
    }

    @Override
    public Completable evict(ProductId id) {
        return Completable.complete();
    }
}
