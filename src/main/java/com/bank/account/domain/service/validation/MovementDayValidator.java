package com.bank.account.domain.service.validation;

import com.bank.account.domain.model.Account;

public class MovementDayValidator implements OpeningValidator {

    @Override
    public void validate(OpeningValidationContext context) {
        Account.requireValidMovementDay(context.requestedType(), context.movementDayOfMonth());
    }
}
