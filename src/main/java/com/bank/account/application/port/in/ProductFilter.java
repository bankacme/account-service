package com.bank.account.application.port.in;

import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;

public record ProductFilter(AccountType accountType, CustomerProfile profile) {

    public static ProductFilter all() {
        return new ProductFilter(null, null);
    }
}
