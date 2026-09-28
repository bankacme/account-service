package com.bank.account.infrastructure.mapper;

import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.ProductId;
import com.bank.account.infrastructure.adapter.out.persistence.AccountProductDocument;
import org.springframework.stereotype.Component;

@Component
public class AccountProductDocumentMapper {

    private final AccountConditionsMapper conditionsMapper;

    public AccountProductDocumentMapper(AccountConditionsMapper conditionsMapper) {
        this.conditionsMapper = conditionsMapper;
    }

    public AccountProductDocument toDocument(AccountProduct product) {
        return AccountProductDocument.builder()
                .id(product.id().value())
                .accountType(product.accountType().name())
                .profile(product.profile().name())
                .conditions(conditionsMapper.toData(product.conditions()))
                .updatedAt(product.updatedAt())
                .build();
    }

    public AccountProduct toDomain(AccountProductDocument document) {
        return new AccountProduct(
                new ProductId(document.getId()),
                AccountType.valueOf(document.getAccountType()),
                CustomerProfile.valueOf(document.getProfile()),
                conditionsMapper.toDomain(document.getConditions()),
                document.getUpdatedAt());
    }
}
