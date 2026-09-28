package com.bank.account.application.port.in;

import com.bank.account.application.command.UpdateAccountCommand;
import com.bank.account.domain.model.Account;
import io.reactivex.rxjava3.core.Single;

public interface UpdateAccountUseCase {

    Single<Account> execute(UpdateAccountCommand command);
}
