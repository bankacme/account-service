package com.bank.account.application.port.in;

import com.bank.account.domain.model.AccountStatus;
import com.bank.account.domain.model.AccountType;

public record AccountFilter(String customerId, AccountType type, AccountStatus status) {

    public static AccountFilter all() {
        return new AccountFilter(null, null, null);
    }
}
