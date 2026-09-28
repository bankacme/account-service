package com.bank.account.application.port.in;

import com.bank.account.application.command.OpenAccountCommand;
import com.bank.account.domain.model.Account;
import io.reactivex.rxjava3.core.Single;

public interface OpenAccountUseCase {

    Single<Account> execute(OpenAccountCommand command);
}
