package com.bank.account.application.usecase;

import com.bank.account.application.port.in.AccountFilter;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountStatus;
import com.bank.account.domain.model.AccountType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryAccountRepository implements AccountRepositoryPort {

    private final Map<String, Account> byId = new LinkedHashMap<>();

    @Override
    public Single<Account> save(Account account) {
        byId.put(account.id().value(), account);
        return Single.just(account);
    }

    @Override
    public Maybe<Account> findById(AccountId id) {
        Account found = byId.get(id.value());
        return found == null ? Maybe.empty() : Maybe.just(found);
    }

    @Override
    public Flowable<Account> findAll(AccountFilter filter) {
        return Flowable.fromIterable(byId.values().stream()
                .filter(account -> filter.customerId() == null || filter.customerId().equals(account.customerId()))
                .filter(account -> filter.type() == null || filter.type() == account.type())
                .filter(account -> filter.status() == null || filter.status() == account.status())
                .toList());
    }

    @Override
    public Single<Long> countActiveByCustomerAndType(String customerId, AccountType type) {
        long count = byId.values().stream()
                .filter(account -> account.customerId().equals(customerId))
                .filter(account -> account.type() == type)
                .filter(account -> account.status() == AccountStatus.ACTIVE)
                .count();
        return Single.just(count);
    }
}
