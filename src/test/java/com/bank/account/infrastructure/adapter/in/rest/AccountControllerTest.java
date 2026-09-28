package com.bank.account.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.bank.account.application.command.UpdateAccountCommand;
import com.bank.account.application.port.in.CloseAccountUseCase;
import com.bank.account.application.port.in.FindAccountByIdUseCase;
import com.bank.account.application.port.in.FindAccountsUseCase;
import com.bank.account.application.port.in.GetBalanceUseCase;
import com.bank.account.application.port.in.OpenAccountUseCase;
import com.bank.account.application.port.in.UpdateAccountUseCase;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.exception.CustomerNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountId;
import com.bank.account.domain.model.BalanceView;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import com.bank.account.infrastructure.mapper.AccountRestMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(AccountController.class)
@Import(AccountRestMapper.class)
class AccountControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private OpenAccountUseCase openAccountUseCase;
    @MockitoBean
    private FindAccountsUseCase findAccountsUseCase;
    @MockitoBean
    private FindAccountByIdUseCase findAccountByIdUseCase;
    @MockitoBean
    private UpdateAccountUseCase updateAccountUseCase;
    @MockitoBean
    private CloseAccountUseCase closeAccountUseCase;
    @MockitoBean
    private GetBalanceUseCase getBalanceUseCase;

    private final Account sampleAccount = AccountFixtures.openPersonalSavings("cust-A", "00100000000001", "100.00");

    @Test
    void openAccountReturns201WithTheOpenedAccount() {
        given(openAccountUseCase.execute(any())).willReturn(Single.just(sampleAccount));

        client.post().uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "customerId": "cust-A",
                          "type": "SAVINGS",
                          "openingAmount": 100.00
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(sampleAccount.id().value())
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void openAccountWithoutARequiredFieldReturns400ValidationError() {
        client.post().uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "type": "SAVINGS"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void openAccountForAMissingCustomerReturns404() {
        given(openAccountUseCase.execute(any()))
                .willReturn(Single.error(new CustomerNotFoundException("missing-customer")));

        client.post().uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "customerId": "missing-customer",
                          "type": "SAVINGS"
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_NOT_FOUND");
    }

    @Test
    void openAccountViolatingABusinessRuleReturns422WithItsSpecificCode() {
        given(openAccountUseCase.execute(any())).willReturn(Single.error(
                new BusinessRuleViolationException("SAVINGS_LIMIT_REACHED", "already has one")));

        client.post().uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "customerId": "cust-A",
                          "type": "SAVINGS"
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SAVINGS_LIMIT_REACHED");
    }

    @Test
    void getAccountReturnsTheAccount() {
        given(findAccountByIdUseCase.execute(eq(sampleAccount.id()))).willReturn(Single.just(sampleAccount));

        client.get().uri("/api/v1/accounts/{id}", sampleAccount.id().value())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo("00100000000001")
                .jsonPath("$.balance").isEqualTo(100.00);
    }

    @Test
    void getAccountThatDoesNotExistReturns404() {
        given(findAccountByIdUseCase.execute(eq(new AccountId("missing"))))
                .willReturn(Single.error(new AccountNotFoundException("missing")));

        client.get().uri("/api/v1/accounts/missing")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    void listAccountsReturnsWhateverTheUseCaseStreams() {
        given(findAccountsUseCase.execute(any())).willReturn(Flowable.just(sampleAccount));

        client.get().uri("/api/v1/accounts?customerId=cust-A")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class).hasSize(1);
    }

    @Test
    void updateAccountReturns200WithTheUpdatedAccount() {
        Account renamed = sampleAccount.updateParties("Ahorros nuevos", java.util.List.of(), java.util.List.of(),
                AccountFixtures.CLOCK);
        given(updateAccountUseCase.execute(any(UpdateAccountCommand.class))).willReturn(Single.just(renamed));

        client.put().uri("/api/v1/accounts/{id}", sampleAccount.id().value())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "alias": "Ahorros nuevos"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.alias").isEqualTo("Ahorros nuevos");
    }

    @Test
    void updateAccountThatConflictsWithAnotherWriteReturns409() {
        given(updateAccountUseCase.execute(any(UpdateAccountCommand.class))).willReturn(Single.error(
                new BusinessRuleViolationException("CONCURRENT_MODIFICATION", "gave up retrying")));

        client.put().uri("/api/v1/accounts/{id}", sampleAccount.id().value())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{ \"alias\": \"x\" }")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONCURRENT_MODIFICATION");
    }

    @Test
    void closeAccountReturns204WithNoBody() {
        given(closeAccountUseCase.execute(sampleAccount.id())).willReturn(Completable.complete());

        client.delete().uri("/api/v1/accounts/{id}", sampleAccount.id().value())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void closeAccountWithANonZeroBalanceReturns422() {
        given(closeAccountUseCase.execute(sampleAccount.id())).willReturn(Completable.error(
                new BusinessRuleViolationException("BALANCE_NOT_ZERO", "balance must be zero")));

        client.delete().uri("/api/v1/accounts/{id}", sampleAccount.id().value())
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("BALANCE_NOT_ZERO");
    }

    @Test
    void getAccountBalanceReturnsTheBalanceWithoutADailyAverageWhenTheConditionHasNone() {
        BalanceView view = new BalanceView(sampleAccount.id().value(), sampleAccount.balance(),
                AccountFixtures.CLOCK.instant(), null);
        given(getBalanceUseCase.execute(eq(sampleAccount.id()))).willReturn(Single.just(view));

        client.get().uri("/api/v1/accounts/{id}/balance", sampleAccount.id().value())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(100.00)
                .jsonPath("$.dailyAverage").doesNotExist();
    }
}
