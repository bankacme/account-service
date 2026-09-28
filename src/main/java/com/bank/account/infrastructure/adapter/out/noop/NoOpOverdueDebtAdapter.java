package com.bank.account.infrastructure.adapter.out.noop;

import com.bank.account.application.port.out.OverdueDebtPort;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Component;

@Component
public class NoOpOverdueDebtAdapter implements OverdueDebtPort {

    @Override
    public Single<Boolean> hasOverdueDebt(String customerId) {
        return Single.just(false);
    }
}
