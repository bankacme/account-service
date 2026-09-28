package com.bank.account.domain.service.validation;

import com.bank.account.domain.model.Account;

public class PartiesValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        Account.requireValidParties(context.customer().type(), context.holders(), context.signers());
    }
}
