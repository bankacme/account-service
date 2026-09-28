package com.bank.account.infrastructure.mapper;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountNumber;
import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.AccountStatus;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.BalanceTracker;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.domain.model.DocumentType;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MonthlyActivity;
import com.bank.account.infrastructure.adapter.out.persistence.AccountDocument;
import com.bank.account.infrastructure.adapter.out.persistence.AccountPartyData;
import com.bank.account.infrastructure.adapter.out.persistence.BalanceTrackerData;
import com.bank.account.infrastructure.adapter.out.persistence.MonthlyActivityData;
import com.bank.account.infrastructure.adapter.out.persistence.PartyDocumentData;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AccountDocumentMapper {

    private final AccountConditionsMapper conditionsMapper;

    public AccountDocumentMapper(AccountConditionsMapper conditionsMapper) {
        this.conditionsMapper = conditionsMapper;
    }

    public AccountDocument toDocument(Account account) {
        return AccountDocument.builder()
                .id(account.id().value())
                .accountNumber(account.accountNumber().value())
                .customerId(account.customerId())
                .customerType(account.customerType().name())
                .customerProfile(account.customerProfile().name())
                .type(account.type().name())
                .alias(account.alias())
                .balance(account.balance().amount())
                .currency(account.balance().currency())
                .conditions(conditionsMapper.toData(account.conditions()))
                .movementDayOfMonth(account.movementDayOfMonth())
                .holders(toPartyDataList(account.holders()))
                .signers(toPartyDataList(account.signers()))
                .monthlyActivity(MonthlyActivityData.builder()
                        .yearMonth(account.monthlyActivity().yearMonth())
                        .movementCount(account.monthlyActivity().movementCount())
                        .build())
                .balanceTracker(BalanceTrackerData.builder()
                        .yearMonth(account.balanceTracker().yearMonth())
                        .accumulatedBalanceDays(account.balanceTracker().accumulatedBalanceDays())
                        .lastBalance(account.balanceTracker().lastBalance().amount())
                        .lastChangeDate(account.balanceTracker().lastChangeDate())
                        .build())
                .status(account.status().name())
                .version(account.version())
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .build();
    }

    public Account toDomain(AccountDocument document) {
        return new Account(
                new AccountId(document.getId()),
                new AccountNumber(document.getAccountNumber()),
                document.getCustomerId(),
                CustomerType.valueOf(document.getCustomerType()),
                CustomerProfile.valueOf(document.getCustomerProfile()),
                AccountType.valueOf(document.getType()),
                document.getAlias(),
                new Money(document.getBalance(), document.getCurrency()),
                conditionsMapper.toDomain(document.getConditions()),
                document.getMovementDayOfMonth(),
                toPartyList(document.getHolders()),
                toPartyList(document.getSigners()),
                new MonthlyActivity(document.getMonthlyActivity().getYearMonth(),
                        document.getMonthlyActivity().getMovementCount()),
                new BalanceTracker(
                        document.getBalanceTracker().getYearMonth(),
                        document.getBalanceTracker().getAccumulatedBalanceDays(),
                        Money.of(document.getBalanceTracker().getLastBalance()),
                        document.getBalanceTracker().getLastChangeDate()),
                AccountStatus.valueOf(document.getStatus()),
                document.getVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private List<AccountPartyData> toPartyDataList(List<AccountParty> parties) {
        return parties.stream()
                .map(party -> AccountPartyData.builder()
                        .document(PartyDocumentData.builder()
                                .type(party.documentType().name())
                                .number(party.documentNumber())
                                .build())
                        .fullName(party.fullName())
                        .build())
                .toList();
    }

    private List<AccountParty> toPartyList(List<AccountPartyData> data) {
        return data.stream()
                .map(party -> new AccountParty(
                        DocumentType.valueOf(party.getDocument().getType()),
                        party.getDocument().getNumber(),
                        party.getFullName()))
                .toList();
    }
}
