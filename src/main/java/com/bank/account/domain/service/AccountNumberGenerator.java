package com.bank.account.domain.service;

import com.bank.account.domain.model.AccountNumber;
import java.util.random.RandomGenerator;

public class AccountNumberGenerator {

    private static final int LENGTH = 14;

    public AccountNumber generate(RandomGenerator random) {
        StringBuilder digits = new StringBuilder(LENGTH);
        digits.append(1 + random.nextInt(9));
        for (int i = 1; i < LENGTH; i++) {
            digits.append(random.nextInt(10));
        }
        return new AccountNumber(digits.toString());
    }
}
