package com.bank.account.domain.service.validation;

import com.bank.account.domain.model.Account;

public class OpeningAmountValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        Account.requireSufficientOpeningAmount(context.openingAmount(), context.conditions());
    }
}
