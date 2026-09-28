package com.bank.account.application.port.in;

import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.ReversalResult;
import io.reactivex.rxjava3.core.Single;

public interface ReverseMovementUseCase {

    Single<ReversalResult> execute(AccountId accountId, String operationId);
}
