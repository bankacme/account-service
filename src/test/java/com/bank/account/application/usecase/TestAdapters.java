package com.bank.account.application.usecase;

import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.application.port.out.CreditCardLookupPort;
import com.bank.account.application.port.out.CustomerLookupPort;
import com.bank.account.application.port.out.OperationLogPort;
import com.bank.account.application.port.out.OverdueDebtPort;
import com.bank.account.application.port.out.ProductCachePort;
import com.bank.account.application.port.out.UnitOfWorkPort;
import com.bank.account.domain.event.AccountDomainEvent;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.ProductId;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;

public final class TestAdapters {

    private TestAdapters() {
    }

    public static final class StubCustomerLookupPort implements CustomerLookupPort {
        private final java.util.Map<String, CustomerSnapshot> byCustomerId = new java.util.HashMap<>();

        public StubCustomerLookupPort with(CustomerSnapshot snapshot) {
            byCustomerId.put(snapshot.customerId(), snapshot);
            return this;
        }

        @Override
        public Maybe<CustomerSnapshot> findById(String customerId) {
            CustomerSnapshot found = byCustomerId.get(customerId);
            return found == null ? Maybe.empty() : Maybe.just(found);
        }
    }

    public static final class FixedCreditCardLookupPort implements CreditCardLookupPort {
        private final boolean hasActiveCreditCard;

        public FixedCreditCardLookupPort(boolean hasActiveCreditCard) {
            this.hasActiveCreditCard = hasActiveCreditCard;
        }

        @Override
        public Single<Boolean> hasActiveCreditCard(String customerId) {
            return Single.just(hasActiveCreditCard);
        }
    }

    public static final class NoOpOverdueDebtPort implements OverdueDebtPort {
        @Override
        public Single<Boolean> hasOverdueDebt(String customerId) {
            return Single.just(false);
        }
    }

    public static final class RecordingEventPublisherPort implements AccountEventPublisherPort {
        private final List<AccountDomainEvent> published = new ArrayList<>();

        @Override
        public Completable publish(AccountDomainEvent event) {
            published.add(event);
            return Completable.complete();
        }

        public List<AccountDomainEvent> published() {
            return published;
        }
    }

    public static final class PassthroughUnitOfWorkPort implements UnitOfWorkPort {
        private final AccountRepositoryPort accountRepositoryPort;
        private final OperationLogPort operationLogPort;

        public PassthroughUnitOfWorkPort(AccountRepositoryPort accountRepositoryPort,
                                          OperationLogPort operationLogPort) {
            this.accountRepositoryPort = accountRepositoryPort;
            this.operationLogPort = operationLogPort;
        }

        @Override
        public Single<Account> saveAccountAndOperation(Account account, AccountOperation operation) {
            return accountRepositoryPort.save(account)
                    .flatMap(saved -> operationLogPort.save(operation).map(savedOperation -> saved));
        }
    }

    public static final class NoOpProductCachePort implements ProductCachePort {
        @Override
        public Maybe<AccountProduct> get(ProductId id) {
            return Maybe.empty();
        }

        @Override
        public Completable put(AccountProduct product) {
            return Completable.complete();
        }

        @Override
        public Completable evict(ProductId id) {
            return Completable.complete();
        }
    }
}
