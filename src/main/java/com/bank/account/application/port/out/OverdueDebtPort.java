package com.bank.account.application.port.out;

import io.reactivex.rxjava3.core.Single;

public interface OverdueDebtPort {

    Single<Boolean> hasOverdueDebt(String customerId);
}
