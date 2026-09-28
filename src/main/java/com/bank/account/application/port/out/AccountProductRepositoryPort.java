package com.bank.account.application.port.out;

import com.bank.account.application.port.in.ProductFilter;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.ProductId;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface AccountProductRepositoryPort {

    Maybe<AccountProduct> findByTypeAndProfile(AccountType type, CustomerProfile profile);

    Flowable<AccountProduct> findAll(ProductFilter filter);

    Single<AccountProduct> save(AccountProduct product);

    Maybe<AccountProduct> findById(ProductId id);
}
