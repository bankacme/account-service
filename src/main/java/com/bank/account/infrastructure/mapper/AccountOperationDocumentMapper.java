package com.bank.account.infrastructure.mapper;

import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MovementType;
import com.bank.account.domain.model.OperationStatus;
import com.bank.account.infrastructure.adapter.out.persistence.AccountOperationDocument;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class AccountOperationDocumentMapper {

    public AccountOperationDocument toDocument(AccountOperation operation) {
        return AccountOperationDocument.builder()
                .id(operation.operationId())
                .accountId(operation.accountId().value())
                .type(operation.type().name())
                .amount(operation.amount().amount())
                .date(operation.date())
                .yearMonth(operation.yearMonth())
                .status(operation.status().name())
                .fee(amountOf(operation.fee()))
                .newBalance(amountOf(operation.newBalance()))
                .movementNumber(operation.movementNumber())
                .reasonCode(operation.reasonCode())
                .reversedBalance(amountOf(operation.reversedBalance()))
                .createdAt(operation.createdAt())
                .updatedAt(operation.updatedAt())
                .build();
    }

    public AccountOperation toDomain(AccountOperationDocument document) {
        return new AccountOperation(
                document.getId(),
                new AccountId(document.getAccountId()),
                MovementType.valueOf(document.getType()),
                Money.of(document.getAmount()),
                document.getDate(),
                document.getYearMonth(),
                OperationStatus.valueOf(document.getStatus()),
                moneyOf(document.getFee()),
                moneyOf(document.getNewBalance()),
                document.getMovementNumber(),
                document.getReasonCode(),
                moneyOf(document.getReversedBalance()),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private BigDecimal amountOf(Money money) {
        return money == null ? null : money.amount();
    }

    private Money moneyOf(BigDecimal amount) {
        return amount == null ? null : Money.of(amount);
    }
}
