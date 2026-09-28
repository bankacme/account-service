package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.application.command.OpenAccountCommand;
import com.bank.account.application.usecase.TestAdapters.FixedCreditCardLookupPort;
import com.bank.account.application.usecase.TestAdapters.NoOpOverdueDebtPort;
import com.bank.account.application.usecase.TestAdapters.RecordingEventPublisherPort;
import com.bank.account.application.usecase.TestAdapters.StubCustomerLookupPort;
import com.bank.account.domain.event.AccountCreated;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.exception.CustomerNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountParty;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.domain.model.DocumentType;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.service.AccountNumberGenerator;
import com.bank.account.domain.service.AccountOpeningPolicy;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class OpenAccountUseCaseImplTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private final InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
    private final InMemoryAccountProductRepository productRepository = new InMemoryAccountProductRepository();
    private final StubCustomerLookupPort customerLookupPort = new StubCustomerLookupPort();
    private final RecordingEventPublisherPort eventPublisherPort = new RecordingEventPublisherPort();

    private final AccountConditions savingsStandard = new AccountConditions(
            Money.zero(), Money.zero(), 10, 5, Money.of(new BigDecimal("2.00")), null, false);

    private OpenAccountUseCaseImpl newUseCase(boolean hasActiveCreditCard) {
        productRepository.seed(AccountProduct.create(AccountType.SAVINGS, CustomerProfile.STANDARD,
                savingsStandard, clock));
        return new OpenAccountUseCaseImpl(customerLookupPort, new NoOpOverdueDebtPort(),
                new FixedCreditCardLookupPort(hasActiveCreditCard), accountRepository, productRepository,
                eventPublisherPort, new AccountOpeningPolicy(), new AccountNumberGenerator(), new Random(42), clock);
    }

    @Test
    void opensAnAccountAndPublishesAccountCreated() {
        customerLookupPort.with(new CustomerSnapshot("cust-A", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                "ACTIVE"));
        OpenAccountUseCaseImpl useCase = newUseCase(false);
        OpenAccountCommand command = new OpenAccountCommand("cust-A", AccountType.SAVINGS, "Ahorros", Money.zero(),
                null, List.of(), List.of());

        TestObserver<Account> observer = useCase.execute(command).test();

        observer.assertComplete();
        observer.assertValueCount(1);
        assertThat(eventPublisherPort.published()).hasSize(1);
        assertThat(eventPublisherPort.published().get(0)).isInstanceOf(AccountCreated.class);
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // customerLookupPort has nothing registered for "ghost": Maybe.empty() -> context.customer() == null
        // -> CustomerExistsValidator (chain step 1) throws, not the use case itself.
        OpenAccountUseCaseImpl useCase = newUseCase(false);
        OpenAccountCommand command = new OpenAccountCommand("ghost", AccountType.SAVINGS, null, Money.zero(),
                null, List.of(), List.of());

        useCase.execute(command).test().assertError(CustomerNotFoundException.class);
    }

    @Test
    void fallsBackToStandardConditionsWhenTheCustomersProfileHasNoCatalogEntry() {
        // Only SAVINGS/STANDARD is seeded; a VIP customer should still open with those
        // conditions (contract: "si no existe la combinación, se usa STANDARD del mismo tipo").
        customerLookupPort.with(new CustomerSnapshot("cust-B", CustomerType.PERSONAL, CustomerProfile.VIP, "ACTIVE"));
        OpenAccountUseCaseImpl useCase = newUseCase(false);
        OpenAccountCommand command = new OpenAccountCommand("cust-B", AccountType.SAVINGS, null,
                Money.of(new BigDecimal("50.00")), null, List.of(), List.of());

        TestObserver<Account> observer = useCase.execute(command).test();

        observer.assertComplete();
        assertThat(observer.values().get(0).conditions()).isEqualTo(savingsStandard);
    }

    @Test
    void rejectsABusinessAccountOpenedAsSavings() {
        customerLookupPort.with(new CustomerSnapshot("cust-C", CustomerType.BUSINESS, CustomerProfile.STANDARD,
                "ACTIVE"));
        OpenAccountUseCaseImpl useCase = newUseCase(false);
        AccountParty holder = new AccountParty(DocumentType.RUC, "20123456789", "Empresa SAC");
        OpenAccountCommand command = new OpenAccountCommand("cust-C", AccountType.SAVINGS, null, Money.zero(),
                null, List.of(holder), List.of());

        useCase.execute(command).test().assertError(BusinessRuleViolationException.class);
    }
}
