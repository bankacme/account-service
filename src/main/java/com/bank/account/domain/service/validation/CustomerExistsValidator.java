package com.bank.account.domain.service.validation;

import com.bank.account.domain.exception.CustomerNotFoundException;

public class CustomerExistsValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        if (context.customer() == null) {
            throw new CustomerNotFoundException(context.customerId());
        }
    }
}
