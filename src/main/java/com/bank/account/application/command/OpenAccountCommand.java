package com.bank.account.application.command;

import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.Money;
import java.util.List;

public record OpenAccountCommand(
        String customerId,
        AccountType type,
        String alias,
        Money openingAmount,
        Integer movementDayOfMonth,
        List<AccountParty> holders,
        List<AccountParty> signers) {

    public OpenAccountCommand {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        if (type == null || openingAmount == null) {
            throw new IllegalArgumentException("type and openingAmount are required");
        }
        holders = holders == null ? List.of() : List.copyOf(holders);
        signers = signers == null ? List.of() : List.copyOf(signers);
    }
}
