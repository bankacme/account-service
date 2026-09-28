package com.bank.account.infrastructure.mapper;

import com.bank.account.application.command.ApplyMovementCommand;
import com.bank.account.application.command.OpenAccountCommand;
import com.bank.account.application.command.UpdateAccountCommand;
import com.bank.account.application.command.UpdateConditionsCommand;
import com.bank.account.application.port.in.AccountFilter;
import com.bank.account.application.port.in.ProductFilter;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.BalanceView;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.DailyAverage;
import com.bank.account.domain.model.DocumentType;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MonthlyActivity;
import com.bank.account.domain.model.MovementResult;
import com.bank.account.domain.model.MovementType;
import com.bank.account.domain.model.ProductId;
import com.bank.account.domain.model.ReversalResult;
import com.bank.account.infrastructure.adapter.in.rest.dto.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * REST DTOs (generated from the contract) &lt;-&gt; application/domain types. Hand-written,
 * same reasoning as the R4 persistence mappers: almost every field needs VO-unwrapping.
 *
 * <p>{@code openingAmount}'s "default 0 if omitted" (contract) is decided HERE, not in the
 * domain or the use case ({@link OpenAccountCommand}'s own javadoc says so): by the time a
 * command exists, the amount is never null.
 *
 * <p>{@code currency} on {@code Account}/{@code BalanceView} is an INLINE enum in the
 * contract (not a named, $ref'd schema like {@code MovementType}), so the generator nests it
 * inside each model as {@code Account.CurrencyEnum}/{@code BalanceView.CurrencyEnum} rather
 * than a shared top-level type — if a real build shows a different generated shape here
 * (name or package), this is the one spot in this file most likely to need a one-line fix.
 */
@Component
public class AccountRestMapper {

    // ---- commands (REST request -> application) ----

    public OpenAccountCommand toCommand(com.bank.account.infrastructure.adapter.in.rest.dto.OpenAccountRequest dto) {
        BigDecimal openingAmount = dto.getOpeningAmount() == null ? BigDecimal.ZERO : dto.getOpeningAmount();
        return new OpenAccountCommand(
                dto.getCustomerId(),
                AccountType.valueOf(dto.getType().name()),
                dto.getAlias(),
                Money.of(openingAmount),
                dto.getMovementDayOfMonth(),
                toPartyDomainList(dto.getHolders()),
                toPartyDomainList(dto.getSigners()));
    }

    public UpdateAccountCommand toCommand(String accountId,
           com.bank.account.infrastructure.adapter.in.rest.dto.UpdateAccountRequest dto) {
        return new UpdateAccountCommand(new AccountId(accountId), dto.getAlias(),
                toPartyDomainList(dto.getHolders()), toPartyDomainList(dto.getSigners()));
    }

    public UpdateConditionsCommand toCommand(String productId,
           com.bank.account.infrastructure.adapter.in.rest.dto.AccountConditions dto) {
        return new UpdateConditionsCommand(new ProductId(productId), toConditions(dto));
    }

    public ApplyMovementCommand toCommand(AccountId accountId,
           com.bank.account.infrastructure.adapter.in.rest.dto.ApplyMovementRequest dto) {
        return new ApplyMovementCommand(accountId, dto.getOperationId(), MovementType.valueOf(dto.getType().name()),
                Money.of(dto.getAmount()), dto.getDate());
    }

    public AccountFilter toFilter(String customerId,
                                  com.bank.account.infrastructure.adapter.in.rest.dto.AccountType type,
                                  com.bank.account.infrastructure.adapter.in.rest.dto.AccountStatus status) {
        return new AccountFilter(customerId,
                type == null ? null : AccountType.valueOf(type.name()),
                status == null ? null : com.bank.account.domain.model.AccountStatus.valueOf(status.name()));
    }

    public ProductFilter toProductFilter(com.bank.account.infrastructure.adapter.in.rest.dto.AccountType accountType,
                                         com.bank.account.infrastructure.adapter.in.rest.dto.CustomerProfile profile) {
        return new ProductFilter(
                accountType == null ? null : AccountType.valueOf(accountType.name()),
                profile == null ? null : CustomerProfile.valueOf(profile.name()));
    }

    // ---- responses (domain -> REST DTO) ----

    public com.bank.account.infrastructure.adapter.in.rest.dto.Account toDto(Account account) {
        com.bank.account.infrastructure.adapter.in.rest.dto.Account dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.Account();
        dto.setId(account.id().value());
        dto.setAccountNumber(account.accountNumber().value());
        dto.setCustomerId(account.customerId());
        dto.setCustomerType(com.bank.account.infrastructure.adapter.in.rest.dto.CustomerType
                .valueOf(account.customerType().name()));
        dto.setCustomerProfile(com.bank.account.infrastructure.adapter.in.rest.dto.CustomerProfile
                .valueOf(account.customerProfile().name()));
        dto.setType(com.bank.account.infrastructure.adapter.in.rest.dto.AccountType.valueOf(account.type().name()));
        dto.setAlias(account.alias());
        dto.setBalance(account.balance().amount());
        dto.setCurrency(com.bank.account.infrastructure.adapter.in.rest.dto.Account.CurrencyEnum
                .valueOf(account.balance().currency()));
        dto.setConditions(toConditionsDto(account.conditions()));
        dto.setMovementDayOfMonth(account.movementDayOfMonth());
        dto.setHolders(toPartyDtoList(account.holders()));
        dto.setSigners(toPartyDtoList(account.signers()));
        dto.setMonthlyActivity(toMonthlyActivityDto(account.monthlyActivity()));
        dto.setStatus(com.bank.account.infrastructure.adapter.in.rest.dto.AccountStatus
                .valueOf(account.status().name()));
        dto.setCreatedAt(toOffsetDateTime(account.createdAt()));
        dto.setUpdatedAt(toOffsetDateTime(account.updatedAt()));
        return dto;
    }

    public com.bank.account.infrastructure.adapter.in.rest.dto.AccountProduct toDto(AccountProduct product) {
        com.bank.account.infrastructure.adapter.in.rest.dto.AccountProduct dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.AccountProduct();
        dto.setId(product.id().value());
        dto.setAccountType(com.bank.account.infrastructure.adapter.in.rest.dto.AccountType
                .valueOf(product.accountType().name()));
        dto.setProfile(com.bank.account.infrastructure.adapter.in.rest.dto.CustomerProfile
                .valueOf(product.profile().name()));
        dto.setConditions(toConditionsDto(product.conditions()));
        dto.setUpdatedAt(toOffsetDateTime(product.updatedAt()));
        return dto;
    }

    public com.bank.account.infrastructure.adapter.in.rest.dto.BalanceView toDto(BalanceView view) {
        com.bank.account.infrastructure.adapter.in.rest.dto.BalanceView dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.BalanceView();
        dto.setAccountId(view.accountId());
        dto.setBalance(view.balance().amount());
        dto.setCurrency(com.bank.account.infrastructure.adapter.in.rest.dto.BalanceView.CurrencyEnum
                .valueOf(view.balance().currency()));
        dto.setAsOf(view.asOf().atOffset(ZoneOffset.UTC));
        if (view.dailyAverage() != null) {
            dto.setDailyAverage(toDailyAverageDto(view.dailyAverage()));
        }
        return dto;
    }

    public com.bank.account.infrastructure.adapter.in.rest.dto.MovementResult toDto(MovementResult result) {
        com.bank.account.infrastructure.adapter.in.rest.dto.MovementResult dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.MovementResult();
        dto.setOperationId(result.operationId());
        dto.setAccountId(result.accountId().value());
        dto.setType(com.bank.account.infrastructure.adapter.in.rest.dto.MovementType.valueOf(result.type().name()));
        dto.setAmount(result.amount().amount());
        dto.setFee(result.fee().amount());
        dto.setNewBalance(result.newBalance().amount());
        dto.setMovementNumber(result.movementNumber());
        return dto;
    }

    public com.bank.account.infrastructure.adapter.in.rest.dto.ReversalResult toDto(ReversalResult result) {
        com.bank.account.infrastructure.adapter.in.rest.dto.ReversalResult dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.ReversalResult();
        dto.setOperationId(result.operationId());
        dto.setAccountId(result.accountId().value());
        dto.setNewBalance(result.newBalance().amount());
        return dto;
    }

    // ---- shared helpers ----

    private AccountConditions toConditions(
            com.bank.account.infrastructure.adapter.in.rest.dto.AccountConditions dto) {
        BigDecimal minimumDailyAverageAmount = dto.getMinimumDailyAverage();
        Money minimumDailyAverage = minimumDailyAverageAmount == null ? null : Money.of(minimumDailyAverageAmount);
        return new AccountConditions(
                Money.of(dto.getMaintenanceFee()),
                Money.of(dto.getMinimumOpeningAmount()),
                dto.getMonthlyMovementLimit(),
                dto.getFreeTransactionsLimit(),
                Money.of(dto.getTransactionFee()),
                minimumDailyAverage,
                Boolean.TRUE.equals(dto.getRequiresCreditCard()));
    }

    private com.bank.account.infrastructure.adapter.in.rest.dto.AccountConditions toConditionsDto(
            AccountConditions conditions) {
        com.bank.account.infrastructure.adapter.in.rest.dto.AccountConditions dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.AccountConditions();
        dto.setMaintenanceFee(conditions.maintenanceFee().amount());
        dto.setMinimumOpeningAmount(conditions.minimumOpeningAmount().amount());
        dto.setMonthlyMovementLimit(conditions.monthlyMovementLimit());
        dto.setFreeTransactionsLimit(conditions.freeTransactionsLimit());
        dto.setTransactionFee(conditions.transactionFee().amount());
        dto.setMinimumDailyAverage(
                conditions.minimumDailyAverage() == null ? null : conditions.minimumDailyAverage().amount());
        dto.setRequiresCreditCard(conditions.requiresCreditCard());
        return dto;
    }

    private com.bank.account.infrastructure.adapter.in.rest.dto.DailyAverage toDailyAverageDto(DailyAverage average) {
        com.bank.account.infrastructure.adapter.in.rest.dto.DailyAverage dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.DailyAverage();
        dto.setYearMonth(average.yearMonth().toString());
        dto.setAverage(average.average().amount());
        dto.setMinimum(average.minimum().amount());
        dto.setMeetsMinimum(average.meetsMinimum());
        return dto;
    }

    private com.bank.account.infrastructure.adapter.in.rest.dto.MonthlyActivity toMonthlyActivityDto(
            MonthlyActivity activity) {
        com.bank.account.infrastructure.adapter.in.rest.dto.MonthlyActivity dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.MonthlyActivity();
        dto.setYearMonth(activity.yearMonth().toString());
        dto.setMovementCount(activity.movementCount());
        return dto;
    }

    private AccountParty toParty(com.bank.account.infrastructure.adapter.in.rest.dto.AccountParty dto) {
        return new AccountParty(DocumentType.valueOf(dto.getDocument().getType().name()), dto.getDocument().getNumber(),
                dto.getFullName());
    }

    private com.bank.account.infrastructure.adapter.in.rest.dto.AccountParty toPartyDto(AccountParty party) {
        Document documentDto = new Document();
        documentDto.setType(com.bank.account.infrastructure.adapter.in.rest.dto.DocumentType
                .valueOf(party.documentType().name()));
        documentDto.setNumber(party.documentNumber());
        com.bank.account.infrastructure.adapter.in.rest.dto.AccountParty dto =
                new com.bank.account.infrastructure.adapter.in.rest.dto.AccountParty();
        dto.setDocument(documentDto);
        dto.setFullName(party.fullName());
        return dto;
    }

    private List<AccountParty> toPartyDomainList(
            List<com.bank.account.infrastructure.adapter.in.rest.dto.AccountParty> parties) {
        return parties == null ? List.of() : parties.stream().map(this::toParty).toList();
    }

    private List<com.bank.account.infrastructure.adapter.in.rest.dto.AccountParty> toPartyDtoList(
            List<AccountParty> parties) {
        return parties.stream().map(this::toPartyDto).toList();
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
