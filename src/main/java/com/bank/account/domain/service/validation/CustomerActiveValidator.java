package com.bank.account.domain.service.validation;

import com.bank.account.domain.exception.BusinessRuleViolationException;

public class CustomerActiveValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        if (!context.customer().isActive()) {
            throw new BusinessRuleViolationException("CUSTOMER_INACTIVE",
                    "Customer " + context.customerId() + " is not active");
        }
    }
}
