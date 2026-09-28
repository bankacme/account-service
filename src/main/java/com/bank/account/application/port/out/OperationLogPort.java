package com.bank.account.application.port.out;

import com.bank.account.domain.model.AccountOperation;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface OperationLogPort {

    Maybe<AccountOperation> find(String operationId);

    Single<AccountOperation> save(AccountOperation operation);
}
