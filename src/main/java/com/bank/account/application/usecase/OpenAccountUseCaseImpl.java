package com.bank.account.application.usecase;

import com.bank.account.application.command.OpenAccountCommand;
import com.bank.account.application.port.in.OpenAccountUseCase;
import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.application.port.out.AccountProductRepositoryPort;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.application.port.out.CreditCardLookupPort;
import com.bank.account.application.port.out.CustomerLookupPort;
import com.bank.account.application.port.out.OverdueDebtPort;
import com.bank.account.domain.event.AccountCreated;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountNumber;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.service.AccountNumberGenerator;
import com.bank.account.domain.service.AccountOpeningPolicy;
import com.bank.account.domain.service.validation.OpeningValidationContext;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;
import java.util.Optional;
import java.util.Random;

public class OpenAccountUseCaseImpl implements OpenAccountUseCase {

    private final CustomerLookupPort customerLookupPort;
    private final OverdueDebtPort overdueDebtPort;
    private final CreditCardLookupPort creditCardLookupPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountProductRepositoryPort accountProductRepositoryPort;
    private final AccountEventPublisherPort accountEventPublisherPort;
    private final AccountOpeningPolicy accountOpeningPolicy;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Random random;
    private final Clock clock;

    public OpenAccountUseCaseImpl(CustomerLookupPort customerLookupPort, OverdueDebtPort overdueDebtPort,
                                   CreditCardLookupPort creditCardLookupPort,
                                   AccountRepositoryPort accountRepositoryPort,
                                   AccountProductRepositoryPort accountProductRepositoryPort,
                                   AccountEventPublisherPort accountEventPublisherPort,
                                   AccountOpeningPolicy accountOpeningPolicy,
                                   AccountNumberGenerator accountNumberGenerator, Random random, Clock clock) {
        this.customerLookupPort = customerLookupPort;
        this.overdueDebtPort = overdueDebtPort;
        this.creditCardLookupPort = creditCardLookupPort;
        this.accountRepositoryPort = accountRepositoryPort;
        this.accountProductRepositoryPort = accountProductRepositoryPort;
        this.accountEventPublisherPort = accountEventPublisherPort;
        this.accountOpeningPolicy = accountOpeningPolicy;
        this.accountNumberGenerator = accountNumberGenerator;
        this.random = random;
        this.clock = clock;
    }

    @Override
    public Single<Account> execute(OpenAccountCommand command) {
        return customerLookupPort.findById(command.customerId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(maybeCustomer -> fetchRemainingPrerequisites(command, maybeCustomer))
                .flatMap(context -> Single.fromCallable(() -> {
                    accountOpeningPolicy.validate(context);
                    return buildAccount(command, context);
                }))
                .flatMap(accountRepositoryPort::save)
                .flatMap(saved -> accountEventPublisherPort.publish(AccountCreated.from(saved))
                        .andThen(Single.just(saved)));
    }

    private Single<OpeningValidationContext> fetchRemainingPrerequisites(OpenAccountCommand command,
                                                                          Optional<CustomerSnapshot> maybeCustomer) {
        return overdueDebtPort.hasOverdueDebt(command.customerId())
                .flatMap(hasOverdueDebt -> accountRepositoryPort
                        .countActiveByCustomerAndType(command.customerId(), command.type())
                        .flatMap(activeCount -> resolveConditions(command, maybeCustomer)
                                .flatMap(conditions -> activeCreditCard(command.customerId(), conditions)
                                        .map(hasActiveCreditCard -> new OpeningValidationContext(
                                                command.customerId(), maybeCustomer.orElse(null), hasOverdueDebt,
                                                command.type(), activeCount, command.holders(), command.signers(),
                                                command.movementDayOfMonth(), hasActiveCreditCard, conditions,
                                                command.openingAmount())))));
    }

    /**
     * Solo se consulta credit-service si la condición lo exige (VIP ahorro, PYME corriente): abrir
     * una cuenta STANDARD no depende de que credit-service esté arriba.
     */
    private Single<Boolean> activeCreditCard(String customerId, AccountConditions conditions) {
        return conditions.requiresCreditCard()
                ? creditCardLookupPort.hasActiveCreditCard(customerId)
                : Single.just(false);
    }

    private Single<AccountConditions> resolveConditions(OpenAccountCommand command,
                                                         Optional<CustomerSnapshot> maybeCustomer) {
        CustomerProfile profile = maybeCustomer.map(CustomerSnapshot::profile).orElse(CustomerProfile.STANDARD);
        return accountProductRepositoryPort.findByTypeAndProfile(command.type(), profile)
                .switchIfEmpty(accountProductRepositoryPort.findByTypeAndProfile(command.type(),
                                CustomerProfile.STANDARD)
                        .switchIfEmpty(Single.error(new IllegalStateException(
                                "Missing seeded AccountProduct for " + command.type() + "/STANDARD"))))
                .map(AccountProduct::conditions);
    }

    private Account buildAccount(OpenAccountCommand command, OpeningValidationContext context) {
        AccountNumber accountNumber = accountNumberGenerator.generate(random);
        return Account.open(command.type(), command.customerId(), context.customer().type(),
                context.customer().profile(), command.alias(), command.openingAmount(), context.conditions(),
                command.holders(), command.signers(), command.movementDayOfMonth(), accountNumber, clock);
    }
}
