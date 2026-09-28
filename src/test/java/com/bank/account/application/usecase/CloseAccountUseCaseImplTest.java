package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.application.usecase.TestAdapters.RecordingEventPublisherPort;
import com.bank.account.domain.event.AccountClosed;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.AccountNumber;
import com.bank.account.domain.model.AccountStatus;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.domain.model.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class CloseAccountUseCaseImplTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private final InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
    private final RecordingEventPublisherPort eventPublisherPort = new RecordingEventPublisherPort();
    private final CloseAccountUseCaseImpl useCase = new CloseAccountUseCaseImpl(accountRepository,
            eventPublisherPort, clock);

    private final AccountConditions savingsStandard = new AccountConditions(
            Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")), null, false);

    private Account openAccount(BigDecimal openingBalance) {
        Account account = Account.open(AccountType.SAVINGS, "cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                null, Money.of(openingBalance), savingsStandard, List.of(), List.of(), null,
                new AccountNumber("00100000000001"), clock);
        return accountRepository.save(account).blockingGet();
    }

    @Test
    void closesTheAccountAndPublishesAccountClosed() {
        Account account = openAccount(BigDecimal.ZERO);

        useCase.execute(account.id()).test().assertComplete();

        assertThat(accountRepository.findById(account.id()).blockingGet().status())
                .isEqualTo(AccountStatus.INACTIVE);
        assertThat(eventPublisherPort.published()).hasSize(1);
        assertThat(eventPublisherPort.published().get(0)).isInstanceOf(AccountClosed.class);
    }

    @Test
    void repeatingTheCloseIsIdempotentAndDoesNotPublishAgain() {
        Account account = openAccount(BigDecimal.ZERO);

        useCase.execute(account.id()).test().assertComplete();
        useCase.execute(account.id()).test().assertComplete();

        assertThat(eventPublisherPort.published()).hasSize(1);
    }

    @Test
    void rejectsClosingAnAccountWithANonZeroBalance() {
        Account account = openAccount(new BigDecimal("50.00"));

        useCase.execute(account.id()).test().assertError(BusinessRuleViolationException.class);
        assertThat(eventPublisherPort.published()).isEmpty();
    }

    @Test
    void rejectsAnUnknownAccount() {
        useCase.execute(new AccountId(java.util.UUID.randomUUID().toString())).test()
                .assertError(AccountNotFoundException.class);
    }
}
