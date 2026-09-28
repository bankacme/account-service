package com.bank.account.application.port.out;

import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.ProductId;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

public interface ProductCachePort {

    Maybe<AccountProduct> get(ProductId id);

    Completable put(AccountProduct product);

    Completable evict(ProductId id);
}
