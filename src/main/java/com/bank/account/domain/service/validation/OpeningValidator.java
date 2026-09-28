package com.bank.account.domain.service.validation;

@FunctionalInterface
public interface OpeningValidator {

    void validate(OpeningValidationContext context);
}
