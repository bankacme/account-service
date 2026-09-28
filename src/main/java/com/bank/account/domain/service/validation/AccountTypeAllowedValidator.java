package com.bank.account.domain.service.validation;

import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerType;

public class AccountTypeAllowedValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        if (context.customer().type() == CustomerType.BUSINESS
                && context.requestedType() != AccountType.CHECKING) {
            throw new BusinessRuleViolationException("ACCOUNT_TYPE_NOT_ALLOWED",
                    "A business customer can only open CHECKING accounts");
        }
    }
}
