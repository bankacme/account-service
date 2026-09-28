package com.bank.account.infrastructure.adapter.out.persistence;

import com.bank.account.application.port.in.AccountFilter;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountNumber;
import com.bank.account.domain.model.AccountStatus;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.service.AccountNumberGenerator;
import com.bank.account.infrastructure.mapper.AccountDocumentMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Random;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 3;

    private final AccountMongoRepository repository;
    private final AccountDocumentMapper mapper;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Random random;

    public AccountPersistenceAdapter(AccountMongoRepository repository, AccountDocumentMapper mapper,
                                      AccountNumberGenerator accountNumberGenerator, Random random) {
        this.repository = repository;
        this.mapper = mapper;
        this.accountNumberGenerator = accountNumberGenerator;
        this.random = random;
    }

    @Override
    public Single<Account> save(Account account) {
        return saveWithRetry(account, MAX_ACCOUNT_NUMBER_ATTEMPTS);
    }

    @Override
    public Maybe<Account> findById(AccountId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Flowable<Account> findAll(AccountFilter filter) {
        Flowable<AccountDocument> base = filter.customerId() != null
                ? repository.findByCustomerId(filter.customerId())
                : repository.findAll();

        return base.map(mapper::toDomain)
                .filter(account -> filter.type() == null || account.type() == filter.type())
                .filter(account -> filter.status() == null || account.status() == filter.status());
    }

    @Override
    public Single<Long> countActiveByCustomerAndType(String customerId, AccountType type) {
        return repository.countByCustomerIdAndTypeAndStatus(customerId, type.name(), AccountStatus.ACTIVE.name());
    }

    private Single<Account> saveWithRetry(Account account, int attemptsLeft) {
        return repository.save(mapper.toDocument(account))
                .map(mapper::toDomain)
                .onErrorResumeNext(error -> translateOrRetry(account, error, attemptsLeft));
    }

    private Single<Account> translateOrRetry(Account account, Throwable error, int attemptsLeft) {
        if (!(error instanceof DuplicateKeyException)) {
            return Single.error(error);
        }
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (message.contains("uk_personal_savings")) {
            return Single.error(new BusinessRuleViolationException("SAVINGS_LIMIT_REACHED",
                    "Customer " + account.customerId() + " already has an active savings account"));
        }
        if (message.contains("uk_personal_checking")) {
            return Single.error(new BusinessRuleViolationException("CHECKING_LIMIT_REACHED",
                    "Customer " + account.customerId() + " already has an active checking account"));
        }
        if (message.contains("uk_account_number") && attemptsLeft > 1) {
            return saveWithRetry(withNewAccountNumber(account), attemptsLeft - 1);
        }
        return Single.error(error);
    }

    private Account withNewAccountNumber(Account account) {
        AccountNumber newNumber = accountNumberGenerator.generate(random);
        return new Account(account.id(), newNumber, account.customerId(), account.customerType(),
                account.customerProfile(), account.type(), account.alias(), account.balance(),
                account.conditions(), account.movementDayOfMonth(), account.holders(), account.signers(),
                account.monthlyActivity(), account.balanceTracker(), account.status(), account.version(),
                account.createdAt(), account.updatedAt());
    }
}
