package com.bank.account.infrastructure.adapter.out.noop;

import com.bank.account.application.port.out.CreditCardLookupPort;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Component;

@Component
public class NoOpCreditCardLookupAdapter implements CreditCardLookupPort {

    @Override
    public Single<Boolean> hasActiveCreditCard(String customerId) {
        return Single.just(false);
    }
}
