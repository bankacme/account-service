package com.bank.account.infrastructure.adapter.out.noop;

import com.bank.account.application.port.out.AccountEventPublisherPort;
import com.bank.account.domain.event.AccountDomainEvent;
import io.reactivex.rxjava3.core.Completable;
import org.springframework.stereotype.Component;

@Component
public class NoOpAccountEventPublisher implements AccountEventPublisherPort {

    @Override
    public Completable publish(AccountDomainEvent event) {
        return Completable.complete();
    }
}
