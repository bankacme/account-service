package com.bank.account.infrastructure.config;

import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.application.port.out.AccountProductRepositoryPort;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.application.port.out.CreditCardLookupPort;
import com.bank.account.application.port.out.CustomerLookupPort;
import com.bank.account.application.port.out.OperationLogPort;
import com.bank.account.application.port.out.OverdueDebtPort;
import com.bank.account.application.port.out.ProductCachePort;
import com.bank.account.application.port.out.UnitOfWorkPort;
import com.bank.account.application.usecase.ApplyMovementUseCaseImpl;
import com.bank.account.application.usecase.CloseAccountUseCaseImpl;
import com.bank.account.application.usecase.FindAccountByIdUseCaseImpl;
import com.bank.account.application.usecase.FindAccountsUseCaseImpl;
import com.bank.account.application.usecase.FindConditionsUseCaseImpl;
import com.bank.account.application.usecase.GetBalanceUseCaseImpl;
import com.bank.account.application.usecase.OpenAccountUseCaseImpl;
import com.bank.account.application.usecase.ReverseMovementUseCaseImpl;
import com.bank.account.application.usecase.UpdateAccountUseCaseImpl;
import com.bank.account.application.usecase.UpdateConditionsUseCaseImpl;
import com.bank.account.domain.service.AccountNumberGenerator;
import com.bank.account.domain.service.AccountOpeningPolicy;
import java.time.Clock;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public AccountOpeningPolicy accountOpeningPolicy() {
        return new AccountOpeningPolicy();
    }

    @Bean
    public OpenAccountUseCaseImpl openAccountUseCase(CustomerLookupPort customerLookupPort,
                                                       OverdueDebtPort overdueDebtPort,
                                                       CreditCardLookupPort creditCardLookupPort,
                                                       AccountRepositoryPort accountRepositoryPort,
                                                       AccountProductRepositoryPort accountProductRepositoryPort,
                                                       AccountEventPublisherPort accountEventPublisherPort,
                                                       AccountOpeningPolicy accountOpeningPolicy,
                                                       AccountNumberGenerator accountNumberGenerator, Random random,
                                                       Clock clock) {
        return new OpenAccountUseCaseImpl(customerLookupPort, overdueDebtPort, creditCardLookupPort,
                accountRepositoryPort, accountProductRepositoryPort, accountEventPublisherPort, accountOpeningPolicy,
                accountNumberGenerator, random, clock);
    }

    @Bean
    public FindAccountByIdUseCaseImpl findAccountByIdUseCase(AccountRepositoryPort repositoryPort) {
        return new FindAccountByIdUseCaseImpl(repositoryPort);
    }

    @Bean
    public FindAccountsUseCaseImpl findAccountsUseCase(AccountRepositoryPort repositoryPort) {
        return new FindAccountsUseCaseImpl(repositoryPort);
    }

    @Bean
    public GetBalanceUseCaseImpl getBalanceUseCase(AccountRepositoryPort repositoryPort, Clock clock) {
        return new GetBalanceUseCaseImpl(repositoryPort, clock);
    }

    @Bean
    public UpdateAccountUseCaseImpl updateAccountUseCase(AccountRepositoryPort repositoryPort,
                                                           AccountEventPublisherPort eventPublisherPort,
                                                           Clock clock) {
        return new UpdateAccountUseCaseImpl(repositoryPort, eventPublisherPort, clock);
    }

    @Bean
    public CloseAccountUseCaseImpl closeAccountUseCase(AccountRepositoryPort repositoryPort,
                                                         AccountEventPublisherPort eventPublisherPort, Clock clock) {
        return new CloseAccountUseCaseImpl(repositoryPort, eventPublisherPort, clock);
    }

    @Bean
    public ApplyMovementUseCaseImpl applyMovementUseCase(AccountRepositoryPort accountRepositoryPort,
                                                           OperationLogPort operationLogPort,
                                                           UnitOfWorkPort unitOfWorkPort,
                                                           AccountEventPublisherPort eventPublisherPort, Clock clock) {
        return new ApplyMovementUseCaseImpl(accountRepositoryPort, operationLogPort, unitOfWorkPort,
                eventPublisherPort, clock);
    }

    @Bean
    public ReverseMovementUseCaseImpl reverseMovementUseCase(AccountRepositoryPort accountRepositoryPort,
                                                               OperationLogPort operationLogPort,
                                                               UnitOfWorkPort unitOfWorkPort,
                                                               AccountEventPublisherPort eventPublisherPort,
                                                               Clock clock) {
        return new ReverseMovementUseCaseImpl(accountRepositoryPort, operationLogPort, unitOfWorkPort,
                eventPublisherPort, clock);
    }

    @Bean
    public FindConditionsUseCaseImpl findConditionsUseCase(AccountProductRepositoryPort repositoryPort) {
        return new FindConditionsUseCaseImpl(repositoryPort);
    }

    @Bean
    public UpdateConditionsUseCaseImpl updateConditionsUseCase(AccountProductRepositoryPort repositoryPort,
                                                                 ProductCachePort cachePort, Clock clock) {
        return new UpdateConditionsUseCaseImpl(repositoryPort, cachePort, clock);
    }
}
