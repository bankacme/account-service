package com.bank.account.application.command;

import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MovementType;
import java.time.LocalDate;

public record ApplyMovementCommand(AccountId accountId, String operationId, MovementType type, Money amount,
                                    LocalDate date) {

    public ApplyMovementCommand {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (type == null || amount == null) {
            throw new IllegalArgumentException("type and amount are required");
        }
    }
}
