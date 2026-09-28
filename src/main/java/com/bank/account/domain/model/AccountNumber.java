package com.bank.account.domain.model;

import java.util.regex.Pattern;

public record AccountNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^\\d{14}$");

    public AccountNumber {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Account number must be exactly 14 digits");
        }
    }
}
