package com.bank.account.application.command;

import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountParty;
import java.util.List;

public record UpdateAccountCommand(AccountId accountId, String alias, List<AccountParty> holders,
                                    List<AccountParty> signers) {

    public UpdateAccountCommand {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        holders = holders == null ? List.of() : List.copyOf(holders);
        signers = signers == null ? List.of() : List.copyOf(signers);
    }
}
