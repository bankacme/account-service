package com.bank.account.infrastructure.adapter.out.persistence;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

public interface AccountProductMongoRepository extends RxJava3CrudRepository<AccountProductDocument, String> {

    Maybe<AccountProductDocument> findByAccountTypeAndProfile(String accountType, String profile);

    Flowable<AccountProductDocument> findByAccountType(String accountType);

    Flowable<AccountProductDocument> findByProfile(String profile);
}
