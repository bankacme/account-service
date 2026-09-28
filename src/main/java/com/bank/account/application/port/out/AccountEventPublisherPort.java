package com.bank.account.application.port.out;

import com.bank.account.domain.event.AccountDomainEvent;
import io.reactivex.rxjava3.core.Completable;

public interface AccountEventPublisherPort {

    Completable publish(AccountDomainEvent event);
}
