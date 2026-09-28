package com.bank.account.domain.service.validation;

import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.Money;
import java.util.List;

public record OpeningValidationContext(
        String customerId,
        CustomerSnapshot customer,
        boolean hasOverdueDebt,
        AccountType requestedType,
        long activeAccountsOfRequestedType,
        List<AccountParty> holders,
        List<AccountParty> signers,
        Integer movementDayOfMonth,
        boolean hasActiveCreditCard,
        AccountConditions conditions,
        Money openingAmount) {
}
