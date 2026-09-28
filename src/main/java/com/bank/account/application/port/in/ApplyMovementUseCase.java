package com.bank.account.application.port.in;

import com.bank.account.application.command.ApplyMovementCommand;
import com.bank.account.domain.model.MovementResult;
import io.reactivex.rxjava3.core.Single;

public interface ApplyMovementUseCase {

    Single<MovementResult> execute(ApplyMovementCommand command);
}
