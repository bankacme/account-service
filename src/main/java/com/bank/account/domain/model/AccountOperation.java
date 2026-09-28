package com.bank.account.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

public record AccountOperation(
        String operationId,
        AccountId accountId,
        MovementType type,
        Money amount,
        LocalDate date,
        YearMonth yearMonth,
        OperationStatus status,
        Money fee,
        Money newBalance,
        Integer movementNumber,
        String reasonCode,
        Money reversedBalance,
        Instant createdAt,
        Instant updatedAt) {

    public AccountOperation {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (accountId == null || type == null || amount == null || date == null || yearMonth == null
                || status == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("accountId, type, amount, date, yearMonth, status, createdAt and "
                    + "updatedAt are required");
        }
        if (status == OperationStatus.REJECTED && (reasonCode == null || reasonCode.isBlank())) {
            throw new IllegalArgumentException("A REJECTED operation requires a reasonCode");
        }
        if (status != OperationStatus.REJECTED && reasonCode != null) {
            throw new IllegalArgumentException("reasonCode only applies to a REJECTED operation");
        }
        if (status == OperationStatus.REVERSED && reversedBalance == null) {
            throw new IllegalArgumentException("A REVERSED operation requires reversedBalance");
        }
        if (status != OperationStatus.REVERSED && reversedBalance != null) {
            throw new IllegalArgumentException("reversedBalance only applies to a REVERSED operation");
        }
    }

    public boolean matches(AccountId candidateAccountId, MovementType candidateType, Money candidateAmount) {
        return accountId.equals(candidateAccountId) && type == candidateType && amount.equals(candidateAmount);
    }

    public MovementResult toMovementResult() {
        if (status == OperationStatus.REJECTED) {
            throw new IllegalStateException("toMovementResult() does not apply to a REJECTED operation");
        }
        return new MovementResult(operationId, accountId, type, amount, fee, newBalance, movementNumber);
    }

    public ReversalResult toReversalResult() {
        if (status != OperationStatus.REVERSED) {
            throw new IllegalStateException("toReversalResult() requires a REVERSED operation, was " + status);
        }
        return new ReversalResult(operationId, accountId, reversedBalance);
    }

    public static AccountOperation applied(MovementResult result, LocalDate date, Instant now) {
        return new AccountOperation(result.operationId(), result.accountId(), result.type(), result.amount(), date,
                YearMonth.from(date), OperationStatus.APPLIED, result.fee(), result.newBalance(),
                result.movementNumber(), null, null, now, now);
    }

    public static AccountOperation rejected(String operationId, AccountId accountId, MovementType type, Money amount,
                                             LocalDate date, String reasonCode, Instant now) {
        return new AccountOperation(operationId, accountId, type, amount, date, YearMonth.from(date),
                OperationStatus.REJECTED, null, null, null, reasonCode, null, now, now);
    }

    public AccountOperation reversed(Money reversedBalanceValue, Instant now) {
        return new AccountOperation(operationId, accountId, type, amount, date, yearMonth, OperationStatus.REVERSED,
                fee, newBalance, movementNumber, null, reversedBalanceValue, createdAt, now);
    }
}
