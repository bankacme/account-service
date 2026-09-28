package com.bank.account.application.usecase;

import com.bank.account.application.port.in.ProductFilter;
import com.bank.account.application.port.out.AccountProductRepositoryPort;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.ProductId;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryAccountProductRepository implements AccountProductRepositoryPort {

    private final Map<String, AccountProduct> byId = new LinkedHashMap<>();

    public void seed(AccountProduct product) {
        byId.put(product.id().value(), product);
    }

    @Override
    public Maybe<AccountProduct> findByTypeAndProfile(AccountType type, CustomerProfile profile) {
        return findById(ProductId.of(type, profile));
    }

    @Override
    public Flowable<AccountProduct> findAll(ProductFilter filter) {
        return Flowable.fromIterable(byId.values().stream()
                .filter(product -> filter.accountType() == null || filter.accountType() == product.accountType())
                .filter(product -> filter.profile() == null || filter.profile() == product.profile())
                .toList());
    }

    @Override
    public Single<AccountProduct> save(AccountProduct product) {
        byId.put(product.id().value(), product);
        return Single.just(product);
    }

    @Override
    public Maybe<AccountProduct> findById(ProductId id) {
        AccountProduct found = byId.get(id.value());
        return found == null ? Maybe.empty() : Maybe.just(found);
    }
}
