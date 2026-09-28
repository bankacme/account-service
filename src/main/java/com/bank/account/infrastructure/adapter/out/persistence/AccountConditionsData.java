package com.bank.account.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded {@code conditions} object (data-model.md 2.1 and 2.2) — the exact same shape
 * inside both {@link AccountDocument} and {@link AccountProductDocument}, so this one class
 * is shared between the two rather than duplicated. Money fields are plain {@link
 * BigDecimal}: {@link com.bank.account.infrastructure.config.MongoConfig}'s custom
 * converters store them as {@code Decimal128} without needing a per-field annotation, and
 * currency is not carried here (the demo only has {@code PEN}; {@link AccountDocument} has
 * its own top-level {@code currency} field for the account's balance, per data-model.md
 * 2.1 — {@link com.bank.account.domain.model.AccountConditions}'s own {@code Money} fields
 * are always PEN too, so nothing is lost).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountConditionsData {

    private BigDecimal maintenanceFee;

    private BigDecimal minimumOpeningAmount;

    private Integer monthlyMovementLimit;

    private int freeTransactionsLimit;

    private BigDecimal transactionFee;

    private BigDecimal minimumDailyAverage;

    private boolean requiresCreditCard;
}
