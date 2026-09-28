package com.bank.account.application.port.out;

import com.bank.account.application.port.in.AccountFilter;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface AccountRepositoryPort {

    Single<Account> save(Account account);

    Maybe<Account> findById(AccountId id);

    Flowable<Account> findAll(AccountFilter filter);

    Single<Long> countActiveByCustomerAndType(String customerId, AccountType type);
}
