package com.bank.account.infrastructure.adapter.out.persistence;

import com.bank.account.application.port.out.UnitOfWorkPort;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.infrastructure.mapper.AccountDocumentMapper;
import com.bank.account.infrastructure.mapper.AccountOperationDocumentMapper;
import com.bank.account.infrastructure.support.RxJavaReactorBridge;
import com.mongodb.MongoException;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
public class MongoUnitOfWorkAdapter implements UnitOfWorkPort {

    private static final int MAX_ATTEMPTS = 3;
    private static final String TRANSIENT_TRANSACTION_ERROR = "TransientTransactionError";

    private final ReactiveMongoTemplate mongoTemplate;
    private final TransactionalOperator transactionalOperator;
    private final AccountDocumentMapper accountMapper;
    private final AccountOperationDocumentMapper operationMapper;

    public MongoUnitOfWorkAdapter(ReactiveMongoTemplate mongoTemplate, TransactionalOperator transactionalOperator,
                                  AccountDocumentMapper accountMapper,
                                  AccountOperationDocumentMapper operationMapper) {
        this.mongoTemplate = mongoTemplate;
        this.transactionalOperator = transactionalOperator;
        this.accountMapper = accountMapper;
        this.operationMapper = operationMapper;
    }

    @Override
    public Single<Account> saveAccountAndOperation(Account account, AccountOperation operation) {
        return attempt(account, operation, MAX_ATTEMPTS);
    }

    private Single<Account> attempt(Account account, AccountOperation operation, int attemptsLeft) {
        Mono<Account> twoWriteChain = mongoTemplate.save(accountMapper.toDocument(account))
                .flatMap(savedAccountDocument -> mongoTemplate.save(operationMapper.toDocument(operation))
                        .thenReturn(accountMapper.toDomain(savedAccountDocument)));
        Mono<Account> transactional = transactionalOperator.transactional(twoWriteChain);
        return RxJavaReactorBridge.toSingle(transactional)
                .onErrorResumeNext(error -> {
                    if (!isTransientTransactionError(error)) {
                        return Single.error(error);
                    }
                    if (attemptsLeft > 1) {
                        return attempt(account, operation, attemptsLeft - 1);
                    }
                    return Single.error(new BusinessRuleViolationException("CONCURRENT_MODIFICATION",
                            "Gave up after " + MAX_ATTEMPTS + " attempts due to a write conflict"));
                });
    }

    private boolean isTransientTransactionError(Throwable error) {
        return error instanceof MongoException mongoError
                && mongoError.hasErrorLabel(TRANSIENT_TRANSACTION_ERROR);
    }

    Single<Account> saveAccountForcingFailureAfterward(Account account) {
        Mono<Account> chain = mongoTemplate.save(accountMapper.toDocument(account))
                .then(Mono.<Account>error(new IllegalStateException("forced failure for the rollback test")));
        Mono<Account> transactional = transactionalOperator.transactional(chain);
        return RxJavaReactorBridge.toSingle(transactional);
    }
}
