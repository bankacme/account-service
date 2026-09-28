package com.bank.account.infrastructure.adapter.in.rest;

import com.bank.account.application.port.in.FindConditionsUseCase;
import com.bank.account.application.port.in.UpdateConditionsUseCase;
import com.bank.account.infrastructure.adapter.in.rest.api.AccountConditionsApi;
import com.bank.account.infrastructure.adapter.in.rest.dto.AccountConditions;
import com.bank.account.infrastructure.adapter.in.rest.dto.AccountProduct;
import com.bank.account.infrastructure.adapter.in.rest.dto.AccountType;
import com.bank.account.infrastructure.adapter.in.rest.dto.CustomerProfile;
import com.bank.account.infrastructure.mapper.AccountRestMapper;
import com.bank.account.infrastructure.support.RxJavaReactorBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ConditionsController implements AccountConditionsApi {

    private final FindConditionsUseCase findConditionsUseCase;
    private final UpdateConditionsUseCase updateConditionsUseCase;
    private final AccountRestMapper mapper;

    public ConditionsController(FindConditionsUseCase findConditionsUseCase,
                                 UpdateConditionsUseCase updateConditionsUseCase, AccountRestMapper mapper) {
        this.findConditionsUseCase = findConditionsUseCase;
        this.updateConditionsUseCase = updateConditionsUseCase;
        this.mapper = mapper;
    }

    @Override
    public Mono<ResponseEntity<Flux<AccountProduct>>> listAccountConditions(AccountType accountType,
                                                                             CustomerProfile profile,
                                                                             ServerWebExchange exchange) {
        Flux<AccountProduct> products = RxJavaReactorBridge
                .toFlux(findConditionsUseCase.execute(mapper.toProductFilter(accountType, profile)))
                .map(mapper::toDto);
        return Mono.just(ResponseEntity.ok(products));
    }

    @Override
    public Mono<ResponseEntity<AccountProduct>> updateAccountConditions(String id,
                                                                         Mono<AccountConditions> accountConditions,
                                                                         ServerWebExchange exchange) {
        return accountConditions
                .map(dto -> mapper.toCommand(id, dto))
                .flatMap(command -> RxJavaReactorBridge.toMono(updateConditionsUseCase.execute(command)))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }
}
