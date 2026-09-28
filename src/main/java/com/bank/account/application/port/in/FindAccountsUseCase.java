package com.bank.account.application.port.in;

import com.bank.account.domain.model.Account;
import io.reactivex.rxjava3.core.Flowable;

public interface FindAccountsUseCase {

    Flowable<Account> execute(AccountFilter filter);
}
