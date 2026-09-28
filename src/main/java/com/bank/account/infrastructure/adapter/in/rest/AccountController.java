package com.bank.account.infrastructure.adapter.in.rest;

import com.bank.account.application.port.in.CloseAccountUseCase;
import com.bank.account.application.port.in.FindAccountByIdUseCase;
import com.bank.account.application.port.in.FindAccountsUseCase;
import com.bank.account.application.port.in.GetBalanceUseCase;
import com.bank.account.application.port.in.OpenAccountUseCase;
import com.bank.account.application.port.in.UpdateAccountUseCase;
import com.bank.account.domain.model.AccountId;
import com.bank.account.infrastructure.adapter.in.rest.api.AccountsApi;
import com.bank.account.infrastructure.adapter.in.rest.dto.Account;
import com.bank.account.infrastructure.adapter.in.rest.dto.AccountStatus;
import com.bank.account.infrastructure.adapter.in.rest.dto.AccountType;
import com.bank.account.infrastructure.adapter.in.rest.dto.BalanceView;
import com.bank.account.infrastructure.adapter.in.rest.dto.OpenAccountRequest;
import com.bank.account.infrastructure.adapter.in.rest.dto.UpdateAccountRequest;
import com.bank.account.infrastructure.mapper.AccountRestMapper;
import com.bank.account.infrastructure.support.RxJavaReactorBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class AccountController implements AccountsApi {

    private final OpenAccountUseCase openAccountUseCase;
    private final FindAccountsUseCase findAccountsUseCase;
    private final FindAccountByIdUseCase findAccountByIdUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final CloseAccountUseCase closeAccountUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    private final AccountRestMapper mapper;

    public AccountController(OpenAccountUseCase openAccountUseCase, FindAccountsUseCase findAccountsUseCase,
                              FindAccountByIdUseCase findAccountByIdUseCase, UpdateAccountUseCase updateAccountUseCase,
                              CloseAccountUseCase closeAccountUseCase, GetBalanceUseCase getBalanceUseCase,
                              AccountRestMapper mapper) {
        this.openAccountUseCase = openAccountUseCase;
        this.findAccountsUseCase = findAccountsUseCase;
        this.findAccountByIdUseCase = findAccountByIdUseCase;
        this.updateAccountUseCase = updateAccountUseCase;
        this.closeAccountUseCase = closeAccountUseCase;
        this.getBalanceUseCase = getBalanceUseCase;
        this.mapper = mapper;
    }

    @Override
    public Mono<ResponseEntity<Account>> openAccount(Mono<OpenAccountRequest> openAccountRequest,
                                                       ServerWebExchange exchange) {
        return openAccountRequest
                .map(mapper::toCommand)
                .flatMap(command -> RxJavaReactorBridge.toMono(openAccountUseCase.execute(command)))
                .map(mapper::toDto)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @Override
    public Mono<ResponseEntity<Flux<Account>>> listAccounts(String customerId, AccountType type,
                                                             AccountStatus status, ServerWebExchange exchange) {
        Flux<Account> accounts = RxJavaReactorBridge
                .toFlux(findAccountsUseCase.execute(mapper.toFilter(customerId, type, status)))
                .map(mapper::toDto);
        return Mono.just(ResponseEntity.ok(accounts));
    }

    @Override
    public Mono<ResponseEntity<Account>> getAccount(String id, ServerWebExchange exchange) {
        return RxJavaReactorBridge.toMono(findAccountByIdUseCase.execute(new AccountId(id)))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Account>> updateAccount(String id, Mono<UpdateAccountRequest> updateAccountRequest,
                                                        ServerWebExchange exchange) {
        return updateAccountRequest
                .map(request -> mapper.toCommand(id, request))
                .flatMap(command -> RxJavaReactorBridge.toMono(updateAccountUseCase.execute(command)))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> closeAccount(String id, ServerWebExchange exchange) {
        return RxJavaReactorBridge.toMono(closeAccountUseCase.execute(new AccountId(id)))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<BalanceView>> getAccountBalance(String id, ServerWebExchange exchange) {
        return RxJavaReactorBridge.toMono(getBalanceUseCase.execute(new AccountId(id)))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }
}
