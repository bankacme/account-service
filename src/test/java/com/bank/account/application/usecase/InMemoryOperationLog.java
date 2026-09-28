package com.bank.account.application.usecase;

import com.bank.account.application.port.out.OperationLogPort;
import com.bank.account.domain.model.AccountOperation;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryOperationLog implements OperationLogPort {

    private final Map<String, AccountOperation> byOperationId = new LinkedHashMap<>();

    @Override
    public Maybe<AccountOperation> find(String operationId) {
        AccountOperation found = byOperationId.get(operationId);
        return found == null ? Maybe.empty() : Maybe.just(found);
    }

    @Override
    public Single<AccountOperation> save(AccountOperation operation) {
        byOperationId.put(operation.operationId(), operation);
        return Single.just(operation);
    }

    public int size() {
        return byOperationId.size();
    }
}
