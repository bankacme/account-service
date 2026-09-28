package com.bank.account.application.port.in;

import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.BalanceView;
import io.reactivex.rxjava3.core.Single;

public interface GetBalanceUseCase {

    Single<BalanceView> execute(AccountId id);
}
