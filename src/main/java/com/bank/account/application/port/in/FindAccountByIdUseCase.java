package com.bank.account.application.port.in;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import io.reactivex.rxjava3.core.Single;

public interface FindAccountByIdUseCase {

    Single<Account> execute(AccountId id);
}
