package com.bank.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.account.domain.exception.BusinessRuleViolationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountTest {

    private final AccountConditions savingsStandard = new AccountConditions(
            Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")), null, false);
    private final AccountConditions fixedTermStandard = new AccountConditions(
            Money.zero(), Money.of(new BigDecimal("100.00")), 1, 5, Money.of(new BigDecimal("2.00")), null, false);
    private final AccountConditions checkingPyme = new AccountConditions(
            Money.zero(), Money.zero(), null, 5, Money.of(new BigDecimal("2.00")), null, true);
    private final AccountNumber number = new AccountNumber("00100000000001");

    private Clock clockOn(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    @Test
    void opensAPersonalAccountActiveWithTheOpeningAmountAsBalance() {
        Clock clock = clockOn(LocalDate.of(2026, 9, 1));
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(new BigDecimal("1000.00")), savingsStandard, List.of(), List.of(), null, number, clock);

        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.balance().amount()).isEqualByComparingTo("1000.00");
        assertThat(account.version()).isZero();
    }

    @Test
    void rejectsABusinessAccountWithoutAtLeastOneHolder() {
        Clock clock = clockOn(LocalDate.of(2026, 9, 1));
        assertThatThrownBy(() -> Account.open(AccountType.CHECKING, "cust-E", CustomerType.BUSINESS,
                CustomerProfile.PYME, null, Money.zero(), checkingPyme, List.of(), List.of(), null, number, clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("HOLDER_REQUIRED"));
    }

    @Test
    void rejectsAPersonalAccountWithHolders() {
        Clock clock = clockOn(LocalDate.of(2026, 9, 1));
        AccountParty someone = new AccountParty(DocumentType.DNI, "12345678", "Alguien");
        assertThatThrownBy(() -> Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL,
                CustomerProfile.STANDARD, null, Money.zero(), savingsStandard, List.of(someone), List.of(), null,
                number, clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("PARTIES_NOT_ALLOWED"));
    }

    @Test
    void rejectsAFixedTermAccountWithoutMovementDay() {
        Clock clock = clockOn(LocalDate.of(2026, 9, 1));
        assertThatThrownBy(() -> Account.open(AccountType.FIXED_TERM, "cust-A", CustomerType.PERSONAL,
                CustomerProfile.STANDARD, null, Money.of(new BigDecimal("100.00")), fixedTermStandard, List.of(),
                List.of(), null, number, clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("MOVEMENT_DAY_REQUIRED"));
    }

    @Test
    void rejectsAMovementDayOnANonFixedTermAccount() {
        Clock clock = clockOn(LocalDate.of(2026, 9, 1));
        assertThatThrownBy(() -> Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL,
                CustomerProfile.STANDARD, null, Money.zero(), savingsStandard, List.of(), List.of(), 15, number,
                clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("MOVEMENT_DAY_NOT_ALLOWED"));
    }

    @Test
    void rejectsAnOpeningAmountBelowTheMinimum() {
        Clock clock = clockOn(LocalDate.of(2026, 9, 1));
        assertThatThrownBy(() -> Account.open(AccountType.FIXED_TERM, "cust-A", CustomerType.PERSONAL,
                CustomerProfile.STANDARD, null, Money.of(new BigDecimal("50.00")), fixedTermStandard, List.of(),
                List.of(), 15, number, clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("INSUFFICIENT_OPENING_AMOUNT"));
    }

    @Test
    void chargesTheTransactionFeeStartingOnTheMovementAfterTheFreeLimit() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(new BigDecimal("1000.00")), savingsStandard, List.of(), List.of(), null, number, clock);
        Money ten = Money.of(new BigDecimal("10.00"));

        for (int i = 1; i <= 5; i++) {
            Account.MovementOutcome outcome = account.applyMovement("op-" + i, MovementType.DEPOSIT, ten, day1, clock);
            assertThat(outcome.result().fee().isZero()).as("movement %d should be free", i).isTrue();
            account = outcome.account();
        }

        Account.MovementOutcome sixth = account.applyMovement("op-6", MovementType.DEPOSIT, ten, day1, clock);

        assertThat(sixth.result().fee().amount()).isEqualByComparingTo("2.00");
        assertThat(sixth.result().movementNumber()).isEqualTo(6);
        assertThat(sixth.account().balance().amount()).isEqualByComparingTo("1058.00");
    }

    @Test
    void rejectsTheMovementOnceTheMonthlyLimitIsReached() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);
        Account account = Account.open(AccountType.SAVINGS, "cust-B", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(new BigDecimal("1000.00")), savingsStandard, List.of(), List.of(), null, number, clock);
        Money one = Money.of(new BigDecimal("1.00"));
        for (int i = 1; i <= 10; i++) {
            account = account.applyMovement("op-" + i, MovementType.DEPOSIT, one, day1, clock).account();
        }

        Account finalAccount = account;
        assertThatThrownBy(() -> finalAccount.applyMovement("op-11", MovementType.DEPOSIT, one, day1, clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("MONTHLY_LIMIT_EXCEEDED"));
    }

    @Test
    void fixedTermAcceptsOneMovementOnItsDayAndRejectsAnyOtherDay() {
        LocalDate day15 = LocalDate.of(2026, 9, 15);
        Clock clockDay15 = clockOn(day15);
        Account account = Account.open(AccountType.FIXED_TERM, "cust-C", CustomerType.PERSONAL,
                CustomerProfile.STANDARD, null, Money.of(new BigDecimal("500.00")), fixedTermStandard, List.of(),
                List.of(), 15, number, clockDay15);
        Money fifty = Money.of(new BigDecimal("50.00"));

        Account afterFirst = account.applyMovement("ft-1", MovementType.DEPOSIT, fifty, day15, clockDay15).account();

        assertThatThrownBy(() -> afterFirst.applyMovement("ft-2", MovementType.DEPOSIT, fifty, day15, clockDay15))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("MONTHLY_LIMIT_EXCEEDED"));

        LocalDate day16 = LocalDate.of(2026, 9, 16);
        Clock clockDay16 = clockOn(day16);
        Account other = Account.open(AccountType.FIXED_TERM, "cust-D", CustomerType.PERSONAL,
                CustomerProfile.STANDARD, null, Money.of(new BigDecimal("500.00")), fixedTermStandard, List.of(),
                List.of(), 15, number, clockDay16);
        assertThatThrownBy(() -> other.applyMovement("ft-3", MovementType.DEPOSIT, fifty, day16, clockDay16))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("NOT_ALLOWED_DAY"));
    }

    @Test
    void aWithdrawalThatWouldLeaveTheBalanceNegativeIsRejected() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(new BigDecimal("100.00")), savingsStandard, List.of(), List.of(), null, number, clock);

        assertThatThrownBy(() -> account.applyMovement("op-wd", MovementType.WITHDRAWAL,
                Money.of(new BigDecimal("999.00")), day1, clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("INSUFFICIENT_FUNDS"));
    }

    @Test
    void anInactiveAccountRejectsAnyMovement() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.zero(), savingsStandard, List.of(), List.of(), null, number, clock).close(clock);

        assertThatThrownBy(() -> account.applyMovement("op-1", MovementType.DEPOSIT,
                Money.of(new BigDecimal("10.00")), day1, clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("ACCOUNT_INACTIVE"));
    }

    @Test
    void reversingAWithdrawalRestoresTheBalanceAndDoesNotRequireAnActiveAccount() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);

        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.zero(), savingsStandard, List.of(), List.of(), null, number, clock).close(clock);
        assertThat(account.status()).isEqualTo(AccountStatus.INACTIVE);

        Account.ReversalOutcome reversal = account.reverseMovement("op-1", MovementType.WITHDRAWAL,
                Money.of(new BigDecimal("10.00")), Money.zero(), YearMonth.from(day1), day1, clock);

        assertThat(reversal.result().newBalance().amount()).isEqualByComparingTo("10.00");
        assertThat(reversal.account().status()).isEqualTo(AccountStatus.INACTIVE);
    }

    // --- close() ----------------------------------------------------------------------------

    @Test
    void closingRequiresAZeroBalance() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(new BigDecimal("100.00")), savingsStandard, List.of(), List.of(), null, number, clock);

        assertThatThrownBy(() -> account.close(clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("BALANCE_NOT_ZERO"));
    }

    @Test
    void closingTwiceIsIdempotent() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.zero(), savingsStandard, List.of(), List.of(), null, number, clock);

        Account closedOnce = account.close(clock);
        Account closedTwice = closedOnce.close(clock);

        assertThat(closedOnce.status()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(closedTwice.updatedAt()).isEqualTo(closedOnce.updatedAt());
    }

    @Test
    void updatingPartiesOnAnInactiveAccountIsRejected() {
        LocalDate day1 = LocalDate.of(2026, 9, 1);
        Clock clock = clockOn(day1);
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.zero(), savingsStandard, List.of(), List.of(), null, number, clock).close(clock);

        assertThatThrownBy(() -> account.updateParties("Nuevo alias", List.of(), List.of(), clock))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo("ACCOUNT_INACTIVE"));
    }
}
