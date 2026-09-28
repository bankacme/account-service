package com.bank.account.application.command;

import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.ProductId;

public record UpdateConditionsCommand(ProductId productId, AccountConditions conditions) {

    public UpdateConditionsCommand {
        if (productId == null || conditions == null) {
            throw new IllegalArgumentException("productId and conditions are required");
        }
    }
}
