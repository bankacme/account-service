package com.bank.account.domain.service.validation;

import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerType;

public class AccountLimitValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        if (context.customer().type() != CustomerType.PERSONAL) {
            return;
        }
        if (context.requestedType() == AccountType.SAVINGS && context.activeAccountsOfRequestedType() > 0) {
            throw new BusinessRuleViolationException("SAVINGS_LIMIT_REACHED",
                    "Customer " + context.customerId() + " already has an active savings account");
        }
        if (context.requestedType() == AccountType.CHECKING && context.activeAccountsOfRequestedType() > 0) {
            throw new BusinessRuleViolationException("CHECKING_LIMIT_REACHED",
                    "Customer " + context.customerId() + " already has an active checking account");
        }
    }
}
