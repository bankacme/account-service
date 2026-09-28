package com.bank.account.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.RxJava3CrudRepository;

public interface AccountOperationMongoRepository extends RxJava3CrudRepository<AccountOperationDocument, String> {
}
