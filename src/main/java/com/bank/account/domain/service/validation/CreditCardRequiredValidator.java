package com.bank.account.domain.service.validation;

import com.bank.account.domain.exception.BusinessRuleViolationException;

public class CreditCardRequiredValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        if (context.conditions().requiresCreditCard() && !context.hasActiveCreditCard()) {
            throw new BusinessRuleViolationException("CREDIT_CARD_REQUIRED",
                    "Customer " + context.customerId() + " needs an active credit card for this product");
        }
    }
}
