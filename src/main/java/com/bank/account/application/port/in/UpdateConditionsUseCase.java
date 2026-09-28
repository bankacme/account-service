package com.bank.account.application.port.in;

import com.bank.account.application.command.UpdateConditionsCommand;
import com.bank.account.domain.model.AccountProduct;
import io.reactivex.rxjava3.core.Single;

public interface UpdateConditionsUseCase {

    Single<AccountProduct> execute(UpdateConditionsCommand command);
}
