package com.bank.account.infrastructure.adapter.out.persistence;

import com.bank.account.application.port.out.OperationLogPort;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.infrastructure.mapper.AccountOperationDocumentMapper;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Component;

@Component
public class OperationLogPersistenceAdapter implements OperationLogPort {

    private final AccountOperationMongoRepository repository;
    private final AccountOperationDocumentMapper mapper;

    public OperationLogPersistenceAdapter(AccountOperationMongoRepository repository,
                                           AccountOperationDocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Maybe<AccountOperation> find(String operationId) {
        return repository.findById(operationId).map(mapper::toDomain);
    }

    @Override
    public Single<AccountOperation> save(AccountOperation operation) {
        return repository.save(mapper.toDocument(operation)).map(mapper::toDomain);
    }
}
