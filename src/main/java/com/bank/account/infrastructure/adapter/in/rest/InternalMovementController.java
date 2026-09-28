package com.bank.account.infrastructure.adapter.in.rest;

import com.bank.account.application.port.in.ApplyMovementUseCase;
import com.bank.account.application.port.in.ReverseMovementUseCase;
import com.bank.account.domain.model.AccountId;
import com.bank.account.infrastructure.adapter.in.rest.api.InternalMovementsApi;
import com.bank.account.infrastructure.adapter.in.rest.dto.ApplyMovementRequest;
import com.bank.account.infrastructure.adapter.in.rest.dto.MovementResult;
import com.bank.account.infrastructure.adapter.in.rest.dto.ReversalResult;
import com.bank.account.infrastructure.mapper.AccountRestMapper;
import com.bank.account.infrastructure.support.RxJavaReactorBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class InternalMovementController implements InternalMovementsApi {

    private final ApplyMovementUseCase applyMovementUseCase;
    private final ReverseMovementUseCase reverseMovementUseCase;
    private final AccountRestMapper mapper;

    public InternalMovementController(ApplyMovementUseCase applyMovementUseCase,
                                       ReverseMovementUseCase reverseMovementUseCase, AccountRestMapper mapper) {
        this.applyMovementUseCase = applyMovementUseCase;
        this.reverseMovementUseCase = reverseMovementUseCase;
        this.mapper = mapper;
    }

    @Override
    public Mono<ResponseEntity<MovementResult>> applyMovement(String id,
                                                                Mono<ApplyMovementRequest> applyMovementRequest,
                                                                ServerWebExchange exchange) {
        AccountId accountId = new AccountId(id);
        return applyMovementRequest
                .map(request -> mapper.toCommand(accountId, request))
                .flatMap(command -> RxJavaReactorBridge.toMono(applyMovementUseCase.execute(command)))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ReversalResult>> reverseMovement(String id, String operationId,
                                                                 ServerWebExchange exchange) {
        return RxJavaReactorBridge.toMono(reverseMovementUseCase.execute(new AccountId(id), operationId))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }
}
