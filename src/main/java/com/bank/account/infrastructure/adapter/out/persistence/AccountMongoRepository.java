package com.bank.account.infrastructure.adapter.out.persistence;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

public interface AccountMongoRepository extends RxJava3CrudRepository<AccountDocument, String> {

    Flowable<AccountDocument> findByCustomerId(String customerId);

    Single<Long> countByCustomerIdAndTypeAndStatus(String customerId, String type, String status);
}
