package com.bank.account.infrastructure.config;

import com.bank.account.infrastructure.adapter.out.persistence.AccountDocument;
import com.bank.account.infrastructure.adapter.out.persistence.AccountOperationDocument;
import com.bank.account.infrastructure.adapter.out.persistence.AccountProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountIndexInitializer implements ApplicationRunner {

    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createAccountIndexes();
        createProductIndex();
        createOperationIndex();
    }

    private void createAccountIndexes() {
        Index accountNumber = new Index()
                .on("accountNumber", Sort.Direction.ASC)
                .unique()
                .named("uk_account_number");

        Index customerTypeStatus = new Index()
                .on("customerId", Sort.Direction.ASC)
                .on("type", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("ix_account_customer_type_status");

        Index personalSavings = new Index()
                .on("customerId", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(Criteria.where("customerType").is("PERSONAL")
                        .and("type").is("SAVINGS")
                        .and("status").is("ACTIVE")))
                .named("uk_personal_savings");

        Index personalChecking = new Index()
                .on("customerId", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(Criteria.where("customerType").is("PERSONAL")
                        .and("type").is("CHECKING")
                        .and("status").is("ACTIVE")))
                .named("uk_personal_checking");

        createIndex(AccountDocument.class, accountNumber);
        createIndex(AccountDocument.class, customerTypeStatus);
        createIndex(AccountDocument.class, personalSavings);
        createIndex(AccountDocument.class, personalChecking);
    }

    private void createProductIndex() {
        Index productTypeProfile = new Index()
                .on("accountType", Sort.Direction.ASC)
                .on("profile", Sort.Direction.ASC)
                .unique()
                .named("uk_product_type_profile");

        createIndex(AccountProductDocument.class, productTypeProfile);
    }

    private void createOperationIndex() {
        Index operationAccountCreated = new Index()
                .on("accountId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("ix_operation_account_created");

        createIndex(AccountOperationDocument.class, operationAccountCreated);
    }

    private void createIndex(Class<?> documentType, Index index) {
        mongoTemplate.indexOps(documentType)
                .createIndex(index)
                .subscribe(
                        name -> log.info("Index '{}' ready on {}", name, documentType.getSimpleName()),
                        error -> log.error("Could not create index '{}' on {}", index.getIndexKeys(),
                                documentType.getSimpleName(), error));
    }
}
