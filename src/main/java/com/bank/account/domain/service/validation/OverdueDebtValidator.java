package com.bank.account.domain.service.validation;

import com.bank.account.domain.exception.BusinessRuleViolationException;

public class OverdueDebtValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        if (context.hasOverdueDebt()) {
            throw new BusinessRuleViolationException("OVERDUE_DEBT",
                    "Customer " + context.customerId() + " has overdue debt");
        }
    }
}
