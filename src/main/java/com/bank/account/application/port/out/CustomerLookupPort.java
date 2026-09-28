package com.bank.account.application.port.out;

import com.bank.account.domain.model.CustomerSnapshot;
import io.reactivex.rxjava3.core.Maybe;

public interface CustomerLookupPort {

    Maybe<CustomerSnapshot> findById(String customerId);
}
