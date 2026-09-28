package com.bank.account.application.port.in;

import com.bank.account.domain.model.AccountProduct;
import io.reactivex.rxjava3.core.Flowable;

public interface FindConditionsUseCase {

    Flowable<AccountProduct> execute(ProductFilter filter);
}
