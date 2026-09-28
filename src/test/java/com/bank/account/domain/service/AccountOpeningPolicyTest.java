package com.bank.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.exception.CustomerNotFoundException;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.domain.model.DocumentType;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.service.validation.OpeningValidationContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountOpeningPolicyTest {

    private final AccountOpeningPolicy policy = new AccountOpeningPolicy();
    private final AccountConditions savingsVip = new AccountConditions(
            Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")),
            Money.of(new BigDecimal("500.00")), true);
    private final CustomerSnapshot activePersonalVip =
            new CustomerSnapshot("cust-A", CustomerType.PERSONAL, CustomerProfile.VIP, "ACTIVE");

    @Test
    void rejectsAnUnknownCustomerWithCustomerNotFound() {
        OpeningValidationContext context = new OpeningValidationContext("missing", null, false, AccountType.SAVINGS,
                0, List.of(), List.of(), null, true, savingsVip, Money.zero());

        assertThatThrownBy(() -> policy.validate(context)).isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void rejectsAnInactiveCustomer() {
        CustomerSnapshot inactive = new CustomerSnapshot("cust-A", CustomerType.PERSONAL, CustomerProfile.VIP,
                "INACTIVE");
        OpeningValidationContext context = new OpeningValidationContext("cust-A", inactive, false, AccountType.SAVINGS,
                0, List.of(), List.of(), null, true, savingsVip, Money.zero());

        assertThatThrownBy(() -> policy.validate(context))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("CUSTOMER_INACTIVE"));
    }

    @Test
    void requiresAnActiveCreditCardWhenTheConditionDemandsIt() {
        OpeningValidationContext context = new OpeningValidationContext("cust-A", activePersonalVip, false,
                AccountType.SAVINGS, 0, List.of(), List.of(), null, false, savingsVip, Money.zero());

        assertThatThrownBy(() -> policy.validate(context))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("CREDIT_CARD_REQUIRED"));
    }

    @Test
    void passesWhenEveryRuleIsSatisfied() {
        OpeningValidationContext context = new OpeningValidationContext("cust-A", activePersonalVip, false,
                AccountType.SAVINGS, 0, List.of(), List.of(), null, true, savingsVip,
                Money.of(new BigDecimal("100.00")));

        policy.validate(context); // must not throw
    }

    @Test
    void aBusinessCustomerCanOnlyOpenCheckingAccounts() {
        CustomerSnapshot business = new CustomerSnapshot("cust-E", CustomerType.BUSINESS, CustomerProfile.PYME,
                "ACTIVE");
        AccountConditions checkingPyme = new AccountConditions(
                Money.zero(), Money.zero(), null, 5, Money.of(new BigDecimal("2.00")), null, true);
        OpeningValidationContext context = new OpeningValidationContext("cust-E", business, false, AccountType.SAVINGS,
                0, List.of(), List.of(), null, true, checkingPyme, Money.zero());

        assertThatThrownBy(() -> policy.validate(context))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("ACCOUNT_TYPE_NOT_ALLOWED"));
    }

    @Test
    void aBusinessCheckingAccountRequiresAtLeastOneHolder() {
        CustomerSnapshot business = new CustomerSnapshot("cust-E", CustomerType.BUSINESS, CustomerProfile.PYME,
                "ACTIVE");
        AccountConditions checkingPyme = new AccountConditions(
                Money.zero(), Money.zero(), null, 5, Money.of(new BigDecimal("2.00")), null, true);
        OpeningValidationContext context = new OpeningValidationContext("cust-E", business, false,
                AccountType.CHECKING, 0, List.of(), List.of(), null, true, checkingPyme, Money.zero());

        assertThatThrownBy(() -> policy.validate(context))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("HOLDER_REQUIRED"));
    }

    @Test
    void aPersonalCustomerCannotOpenASecondActiveSavingsAccount() {
        AccountConditions savingsStandard = new AccountConditions(
                Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")), null, false);
        OpeningValidationContext context = new OpeningValidationContext("cust-A", activePersonalVip, false,
                AccountType.SAVINGS, 1, List.of(), List.of(), null, true, savingsStandard, Money.zero());

        assertThatThrownBy(() -> policy.validate(context))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("SAVINGS_LIMIT_REACHED"));
    }

    @Test
    void aPersonalAccountRejectsExtraHoldersEvenIfEverythingElsePasses() {
        AccountParty extra = new AccountParty(DocumentType.DNI, "12345678", "Alguien");
        AccountConditions savingsStandard = new AccountConditions(
                Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")), null, false);
        OpeningValidationContext context = new OpeningValidationContext("cust-A", activePersonalVip, false,
                AccountType.SAVINGS, 0, List.of(extra), List.of(), null, true, savingsStandard, Money.zero());

        assertThatThrownBy(() -> policy.validate(context))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("PARTIES_NOT_ALLOWED"));
    }
}
