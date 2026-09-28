package com.bank.account.infrastructure.config;

import com.bank.account.application.port.out.AccountProductRepositoryPort;
import com.bank.account.domain.model.AccountConditions;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.Money;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountProductSeeder implements ApplicationRunner {

    private final AccountProductRepositoryPort repository;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        Flowable.fromIterable(initialCatalog())
                .concatMapMaybe(this::seedIfMissing)
                .doOnNext(product -> log.info("Seeded account-conditions catalog entry '{}'", product.id().value()))
                .ignoreElements()
                .blockingAwait();
    }

    private Maybe<AccountProduct> seedIfMissing(AccountProduct product) {
        return repository.findById(product.id())
                .isEmpty()
                .filter(missing -> missing)
                .flatMap(missing -> repository.save(product).toMaybe());
    }

    private List<AccountProduct> initialCatalog() {
        return List.of(
                product(AccountType.SAVINGS, CustomerProfile.STANDARD,
                        conditions("0", "0", 10, 5, "2.00", null, false)),
                product(AccountType.SAVINGS, CustomerProfile.VIP,
                        conditions("0", "0", 10, 5, "2.00", "500.00", true)),
                product(AccountType.CHECKING, CustomerProfile.STANDARD,
                        conditions("15.00", "0", null, 5, "2.00", null, false)),
                product(AccountType.CHECKING, CustomerProfile.PYME,
                        conditions("0", "0", null, 5, "2.00", null, true)),
                product(AccountType.FIXED_TERM, CustomerProfile.STANDARD,
                        conditions("0", "100.00", 1, 5, "2.00", null, false)));
    }

    private AccountProduct product(AccountType type, CustomerProfile profile, AccountConditions conditions) {
        return AccountProduct.create(type, profile, conditions, clock);
    }

    private AccountConditions conditions(String maintenanceFee, String minimumOpeningAmount,
                                          Integer monthlyMovementLimit, int freeTransactionsLimit,
                                          String transactionFee, String minimumDailyAverage,
                                          boolean requiresCreditCard) {
        return new AccountConditions(
                Money.of(maintenanceFee),
                Money.of(minimumOpeningAmount),
                monthlyMovementLimit,
                freeTransactionsLimit,
                Money.of(transactionFee),
                minimumDailyAverage == null ? null : Money.of(minimumDailyAverage),
                requiresCreditCard);
    }
}
