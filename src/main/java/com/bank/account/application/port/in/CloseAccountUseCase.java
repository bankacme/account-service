package com.bank.account.application.port.in;

import com.bank.account.domain.model.AccountId;
import io.reactivex.rxjava3.core.Completable;

public interface CloseAccountUseCase {

    Completable execute(AccountId id);
}
