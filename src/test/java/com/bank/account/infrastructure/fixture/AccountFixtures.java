package com.bank.account.infrastructure.fixture;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountNumber;
import com.bank.account.domain.model.AccountOperation;
import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.domain.model.DocumentType;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MovementResult;
import com.bank.account.domain.model.MovementType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class AccountFixtures {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);

    private AccountFixtures() {
    }

    public static AccountConditions standardConditions() {
        return new AccountConditions(Money.zero(), Money.zero(), 10, 5, Money.of("2.00"), null, false);
    }

    public static AccountConditions vipSavingsConditions() {
        return new AccountConditions(Money.zero(), Money.zero(), 10, 5, Money.of("2.00"), Money.of("500.00"), true);
    }

    public static Account openPersonalSavings(String customerId, String accountNumber, String openingAmount) {
        return Account.open(AccountType.SAVINGS, customerId, CustomerType.PERSONAL, CustomerProfile.STANDARD,
                "Ahorros", Money.of(openingAmount), standardConditions(), List.of(), List.of(), null,
                new AccountNumber(accountNumber), CLOCK);
    }

    public static Account openPersonalChecking(String customerId, String accountNumber, String openingAmount) {
        return Account.open(AccountType.CHECKING, customerId, CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(openingAmount), standardConditions(), List.of(), List.of(), null,
                new AccountNumber(accountNumber), CLOCK);
    }

    public static Account openBusinessChecking(String customerId, String accountNumber, String openingAmount) {
        AccountParty holder = new AccountParty(DocumentType.RUC, "20512345678", "Bodega San Martin SAC");
        return Account.open(AccountType.CHECKING, customerId, CustomerType.BUSINESS, CustomerProfile.PYME,
                "Cuenta principal", Money.of(openingAmount), standardConditions(), List.of(holder), List.of(), null,
                new AccountNumber(accountNumber), CLOCK);
    }

    public static Account openFixedTerm(String customerId, String accountNumber, String openingAmount, int day) {
        return Account.open(AccountType.FIXED_TERM, customerId, CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(openingAmount), standardConditions(), List.of(), List.of(), day,
                new AccountNumber(accountNumber), CLOCK);
    }

    public static AccountOperation appliedOperation(AccountId accountId, MovementType type, String amount,
                                                      String fee, String newBalance, int movementNumber) {
        MovementResult result = new MovementResult(UUID.randomUUID().toString(), accountId, type, Money.of(amount),
                Money.of(fee), Money.of(newBalance), movementNumber);
        return AccountOperation.applied(result, LocalDate.now(CLOCK), CLOCK.instant());
    }
}
