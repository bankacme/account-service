package com.bank.account.domain.model;

import com.bank.account.domain.exception.BusinessRuleViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record Account(
        AccountId id,
        AccountNumber accountNumber,
        String customerId,
        CustomerType customerType,
        CustomerProfile customerProfile,
        AccountType type,
        String alias,
        Money balance,
        AccountConditions conditions,
        Integer movementDayOfMonth,
        List<AccountParty> holders,
        List<AccountParty> signers,
        MonthlyActivity monthlyActivity,
        BalanceTracker balanceTracker,
        AccountStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    private static final int MAX_ALIAS_LENGTH = 60;

    public Account {
        if (id == null || accountNumber == null || customerId == null || customerId.isBlank()
                || customerType == null || customerProfile == null || type == null || balance == null
                || conditions == null || holders == null || signers == null || monthlyActivity == null
                || balanceTracker == null || status == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("All Account fields (except alias/movementDayOfMonth) are required");
        }
        alias = normalizeAlias(alias);
        holders = List.copyOf(holders);
        signers = List.copyOf(signers);
    }

    public static Account open(AccountType type, String customerId, CustomerType customerType,
                                CustomerProfile customerProfile, String alias, Money openingAmount,
                                AccountConditions conditions, List<AccountParty> holders,
                                List<AccountParty> signers, Integer movementDayOfMonth,
                                AccountNumber accountNumber, Clock clock) {
        List<AccountParty> holdersCopy = List.copyOf(holders);
        List<AccountParty> signersCopy = List.copyOf(signers);
        requireValidParties(customerType, holdersCopy, signersCopy);
        requireValidMovementDay(type, movementDayOfMonth);
        requireSufficientOpeningAmount(openingAmount, conditions);
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        return new Account(AccountId.newId(), accountNumber, customerId, customerType, customerProfile, type,
                alias, openingAmount, conditions, movementDayOfMonth, holdersCopy, signersCopy,
                MonthlyActivity.initial(clock), BalanceTracker.initial(openingAmount, today),
                AccountStatus.ACTIVE, 0L, now, now);
    }

    public Account updateParties(String newAlias, List<AccountParty> newHolders, List<AccountParty> newSigners,
                                  Clock clock) {
        requireActive();
        List<AccountParty> holdersCopy = List.copyOf(newHolders);
        List<AccountParty> signersCopy = List.copyOf(newSigners);
        requireValidParties(customerType, holdersCopy, signersCopy);
        return new Account(id, accountNumber, customerId, customerType, customerProfile, type, newAlias, balance,
                conditions, movementDayOfMonth, holdersCopy, signersCopy, monthlyActivity, balanceTracker, status,
                version, createdAt, clock.instant());
    }

    public MovementOutcome applyMovement(String operationId, MovementType movementType, Money amount,
                                          LocalDate date, Clock clock) {
        requireActive();
        requireValidAmount(amount);
        requireValidDate(date, clock);
        if (type == AccountType.FIXED_TERM) {
            requireAllowedDay(date);
        }
        MonthlyActivity activityThisMonth = monthlyActivity.forMonth(YearMonth.from(date));
        requireWithinMonthlyLimit(activityThisMonth);

        int movementNumber = activityThisMonth.movementCount() + 1;
        Money fee = movementNumber > conditions.freeTransactionsLimit() ? conditions.transactionFee() : Money.zero();
        Money newBalance = computeBalanceAfterMovement(movementType, amount, fee);

        Account updated = new Account(id, accountNumber, customerId, customerType, customerProfile, type, alias,
                newBalance, conditions, movementDayOfMonth, holders, signers,
                activityThisMonth.withCount(movementNumber), balanceTracker.recordChange(newBalance, date),
                status, version, createdAt, clock.instant());
        MovementResult result = new MovementResult(operationId, id, movementType, amount, fee, newBalance,
                movementNumber);
        return new MovementOutcome(updated, result);
    }

    public ReversalOutcome reverseMovement(String operationId, MovementType originalType, Money originalAmount,
                                            Money originalFee, YearMonth originalMovementMonth, LocalDate today,
                                            Clock clock) {
        Money newBalance = originalType == MovementType.WITHDRAWAL
                ? balance.plus(originalAmount).plus(originalFee)
                : reverseDeposit(originalAmount, originalFee);

        MonthlyActivity newActivity = originalMovementMonth.equals(monthlyActivity.yearMonth())
                ? monthlyActivity.withCount(Math.max(0, monthlyActivity.movementCount() - 1))
                : monthlyActivity;

        Account updated = new Account(id, accountNumber, customerId, customerType, customerProfile, type, alias,
                newBalance, conditions, movementDayOfMonth, holders, signers, newActivity,
                balanceTracker.recordChange(newBalance, today), status, version, createdAt, clock.instant());
        return new ReversalOutcome(updated, new ReversalResult(operationId, id, newBalance));
    }

    public Account close(Clock clock) {
        if (!balance.isZero()) {
            throw new BusinessRuleViolationException("BALANCE_NOT_ZERO",
                    "Account " + id.value() + " cannot be closed with a non-zero balance");
        }
        if (status == AccountStatus.INACTIVE) {
            return this;
        }
        return new Account(id, accountNumber, customerId, customerType, customerProfile, type, alias, balance,
                conditions, movementDayOfMonth, holders, signers, monthlyActivity, balanceTracker,
                AccountStatus.INACTIVE, version, createdAt, clock.instant());
    }

    private Money reverseDeposit(Money originalAmount, Money originalFee) {
        Money grossBalance = balance.plus(originalFee);
        if (!grossBalance.isGreaterThanOrEqualTo(originalAmount)) {
            throw new BusinessRuleViolationException("INSUFFICIENT_FUNDS",
                    "Reversing deposit " + originalAmount.amount() + " would leave a negative balance");
        }
        return grossBalance.minus(originalAmount);
    }

    private Money computeBalanceAfterMovement(MovementType movementType, Money amount, Money fee) {
        if (movementType == MovementType.WITHDRAWAL) {
            Money totalDebit = amount.plus(fee);
            if (!balance.isGreaterThanOrEqualTo(totalDebit)) {
                throw new BusinessRuleViolationException("INSUFFICIENT_FUNDS",
                        "Balance " + balance.amount() + " is not enough for a withdrawal of " + amount.amount()
                                + " plus fee " + fee.amount());
            }
            return balance.minus(totalDebit);
        }
        Money grossBalance = balance.plus(amount);
        if (!grossBalance.isGreaterThanOrEqualTo(fee)) {
            throw new BusinessRuleViolationException("INSUFFICIENT_FUNDS",
                    "Deposit " + amount.amount() + " does not cover the fee " + fee.amount());
        }
        return grossBalance.minus(fee);
    }

    private void requireActive() {
        if (status == AccountStatus.INACTIVE) {
            throw new BusinessRuleViolationException("ACCOUNT_INACTIVE", "Account " + id.value() + " is inactive");
        }
    }

    private static void requireValidAmount(Money amount) {
        if (amount.isZero()) {
            throw new BusinessRuleViolationException("INVALID_AMOUNT", "Amount must be greater than zero");
        }
    }

    private void requireValidDate(LocalDate date, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today) || date.isBefore(balanceTracker.lastChangeDate())) {
            throw new BusinessRuleViolationException("INVALID_DATE",
                    "Date " + date + " must not be in the future nor before the last change ("
                            + balanceTracker.lastChangeDate() + ")");
        }
    }

    private void requireAllowedDay(LocalDate date) {
        if (movementDayOfMonth == null || date.getDayOfMonth() != movementDayOfMonth) {
            throw new BusinessRuleViolationException("NOT_ALLOWED_DAY",
                    "Fixed-term movements are only allowed on day " + movementDayOfMonth + " of the month");
        }
    }

    private void requireWithinMonthlyLimit(MonthlyActivity activityThisMonth) {
        Integer limit = conditions.monthlyMovementLimit();
        if (limit != null && activityThisMonth.movementCount() >= limit) {
            throw new BusinessRuleViolationException("MONTHLY_LIMIT_EXCEEDED",
                    "Monthly movement limit (" + limit + ") already reached");
        }
    }

    public static void requireValidParties(CustomerType customerType, List<AccountParty> holders,
                                            List<AccountParty> signers) {
        if (customerType == CustomerType.BUSINESS) {
            if (holders.isEmpty()) {
                throw new BusinessRuleViolationException("HOLDER_REQUIRED",
                        "A business account needs at least one holder");
            }
        } else if (!holders.isEmpty() || !signers.isEmpty()) {
            throw new BusinessRuleViolationException("PARTIES_NOT_ALLOWED",
                    "A personal account does not admit holders or signers");
        }
    }

    public static void requireValidMovementDay(AccountType type, Integer movementDayOfMonth) {
        if (type == AccountType.FIXED_TERM) {
            if (movementDayOfMonth == null || movementDayOfMonth < 1 || movementDayOfMonth > 28) {
                throw new BusinessRuleViolationException("MOVEMENT_DAY_REQUIRED",
                        "A fixed-term account requires movementDayOfMonth between 1 and 28");
            }
        } else if (movementDayOfMonth != null) {
            throw new BusinessRuleViolationException("MOVEMENT_DAY_NOT_ALLOWED",
                    "movementDayOfMonth only applies to a fixed-term account");
        }
    }

    public static void requireSufficientOpeningAmount(Money openingAmount, AccountConditions conditions) {
        if (!openingAmount.isGreaterThanOrEqualTo(conditions.minimumOpeningAmount())) {
            throw new BusinessRuleViolationException("INSUFFICIENT_OPENING_AMOUNT",
                    "Opening amount " + openingAmount.amount() + " is below the minimum "
                            + conditions.minimumOpeningAmount().amount());
        }
    }

    private static String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }
        String trimmed = alias.trim();
        if (trimmed.length() > MAX_ALIAS_LENGTH) {
            throw new IllegalArgumentException("Alias must be at most " + MAX_ALIAS_LENGTH + " characters");
        }
        return trimmed;
    }

    public record MovementOutcome(Account account, MovementResult result) {
    }

    public record ReversalOutcome(Account account, ReversalResult result) {
    }
}
