package com.bank.account.application.usecase;

import com.bank.account.application.port.in.GetBalanceUseCase;
import com.bank.account.application.port.out.AccountRepositoryPort;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.BalanceView;
import com.bank.account.domain.model.DailyAverage;
import com.bank.account.domain.model.Money;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

public class GetBalanceUseCaseImpl implements GetBalanceUseCase {

    private final AccountRepositoryPort repositoryPort;
    private final Clock clock;

    public GetBalanceUseCaseImpl(AccountRepositoryPort repositoryPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    @Override
    public Single<BalanceView> execute(AccountId id) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Single.error(new AccountNotFoundException(id.value())))
                .map(this::toBalanceView);
    }

    private BalanceView toBalanceView(Account account) {
        Money minimum = account.conditions().minimumDailyAverage();
        DailyAverage dailyAverage = null;
        if (minimum != null) {
            LocalDate today = LocalDate.now(clock);
            LocalDate openedDate = LocalDate.ofInstant(account.createdAt(), clock.getZone());
            Money average = Money.of(account.balanceTracker().dailyAverage(today, openedDate));
            dailyAverage = DailyAverage.of(YearMonth.from(today), average, minimum);
        }
        return new BalanceView(account.id().value(), account.balance(), clock.instant(), dailyAverage);
    }
}
