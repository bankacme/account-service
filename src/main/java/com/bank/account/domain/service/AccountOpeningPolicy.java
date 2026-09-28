package com.bank.account.domain.service;

import com.bank.account.domain.service.validation.AccountLimitValidator;
import com.bank.account.domain.service.validation.AccountTypeAllowedValidator;
import com.bank.account.domain.service.validation.CreditCardRequiredValidator;
import com.bank.account.domain.service.validation.CustomerActiveValidator;
import com.bank.account.domain.service.validation.CustomerExistsValidator;
import com.bank.account.domain.service.validation.MovementDayValidator;
import com.bank.account.domain.service.validation.OpeningAmountValidator;
import com.bank.account.domain.service.validation.OpeningValidationContext;
import com.bank.account.domain.service.validation.OpeningValidator;
import com.bank.account.domain.service.validation.OverdueDebtValidator;
import com.bank.account.domain.service.validation.PartiesValidator;
import java.util.List;

public class AccountOpeningPolicy {

    private final List<OpeningValidator> chain = List.of(
            new CustomerExistsValidator(),
            new CustomerActiveValidator(),
            new OverdueDebtValidator(),
            new AccountTypeAllowedValidator(),
            new AccountLimitValidator(),
            new PartiesValidator(),
            new MovementDayValidator(),
            new CreditCardRequiredValidator(),
            new OpeningAmountValidator());

    public void validate(OpeningValidationContext context) {
        chain.forEach(validator -> validator.validate(context));
    }
}
