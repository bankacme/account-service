package com.bank.account.application.port.out;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountOperation;
import io.reactivex.rxjava3.core.Single;

public interface UnitOfWorkPort {

    Single<Account> saveAccountAndOperation(Account account, AccountOperation operation);
}
