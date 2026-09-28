package com.bank.account.infrastructure.adapter.out.persistence;

import com.bank.account.application.port.in.ProductFilter;
import com.bank.account.application.port.out.AccountProductRepositoryPort;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.ProductId;
import com.bank.account.infrastructure.mapper.AccountProductDocumentMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Component;

@Component
public class AccountProductPersistenceAdapter implements AccountProductRepositoryPort {

    private final AccountProductMongoRepository repository;
    private final AccountProductDocumentMapper mapper;

    public AccountProductPersistenceAdapter(AccountProductMongoRepository repository,
                                             AccountProductDocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Maybe<AccountProduct> findByTypeAndProfile(AccountType type, CustomerProfile profile) {
        return repository.findByAccountTypeAndProfile(type.name(), profile.name()).map(mapper::toDomain);
    }

    @Override
    public Flowable<AccountProduct> findAll(ProductFilter filter) {
        Flowable<AccountProductDocument> base;
        if (filter.accountType() != null && filter.profile() != null) {
            base = repository.findByAccountTypeAndProfile(filter.accountType().name(), filter.profile().name())
                    .toFlowable();
        } else if (filter.accountType() != null) {
            base = repository.findByAccountType(filter.accountType().name());
        } else if (filter.profile() != null) {
            base = repository.findByProfile(filter.profile().name());
        } else {
            base = repository.findAll();
        }

        return base.map(mapper::toDomain)
                .filter(product -> filter.accountType() == null || product.accountType() == filter.accountType())
                .filter(product -> filter.profile() == null || product.profile() == filter.profile());
    }

    @Override
    public Single<AccountProduct> save(AccountProduct product) {
        return repository.save(mapper.toDocument(product)).map(mapper::toDomain);
    }

    @Override
    public Maybe<AccountProduct> findById(ProductId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }
}
