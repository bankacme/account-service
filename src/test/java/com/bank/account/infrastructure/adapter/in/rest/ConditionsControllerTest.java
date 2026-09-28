package com.bank.account.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bank.account.application.command.UpdateConditionsCommand;
import com.bank.account.application.port.in.FindConditionsUseCase;
import com.bank.account.application.port.in.UpdateConditionsUseCase;
import com.bank.account.domain.exception.ConditionsNotFoundException;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import com.bank.account.infrastructure.mapper.AccountRestMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(ConditionsController.class)
@Import(AccountRestMapper.class)
class ConditionsControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private FindConditionsUseCase findConditionsUseCase;
    @MockitoBean
    private UpdateConditionsUseCase updateConditionsUseCase;

    private final AccountProduct sampleProduct = AccountProduct.create(AccountType.SAVINGS, CustomerProfile.STANDARD,
            AccountFixtures.standardConditions(), AccountFixtures.CLOCK);

    @Test
    void listAccountConditionsReturnsWhateverTheUseCaseStreams() {
        given(findConditionsUseCase.execute(any())).willReturn(Flowable.just(sampleProduct));

        client.get().uri("/api/v1/account-conditions")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class).hasSize(1);
    }

    @Test
    void updateAccountConditionsReturns200WithTheUpdatedEntry() {
        AccountProduct updated = sampleProduct.withConditions(AccountFixtures.vipSavingsConditions(),
                AccountFixtures.CLOCK);
        given(updateConditionsUseCase.execute(any(UpdateConditionsCommand.class))).willReturn(Single.just(updated));

        client.put().uri("/api/v1/account-conditions/{id}", sampleProduct.id().value())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "maintenanceFee": 0.00,
                          "minimumOpeningAmount": 0.00,
                          "monthlyMovementLimit": 10,
                          "freeTransactionsLimit": 5,
                          "transactionFee": 2.00,
                          "minimumDailyAverage": 500.00,
                          "requiresCreditCard": true
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.conditions.requiresCreditCard").isEqualTo(true);
    }

    @Test
    void updateAccountConditionsForAMissingEntryReturns404() {
        given(updateConditionsUseCase.execute(any(UpdateConditionsCommand.class)))
                .willReturn(Single.error(new ConditionsNotFoundException("SAVINGS_STANDARD")));

        client.put().uri("/api/v1/account-conditions/{id}", "SAVINGS_STANDARD")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "maintenanceFee": 0.00,
                          "minimumOpeningAmount": 0.00,
                          "freeTransactionsLimit": 5,
                          "transactionFee": 2.00,
                          "requiresCreditCard": false
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONDITIONS_NOT_FOUND");
    }

    @Test
    void updateAccountConditionsWithoutARequiredFieldReturns400ValidationError() {
        client.put().uri("/api/v1/account-conditions/{id}", "SAVINGS_STANDARD")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{ \"maintenanceFee\": 0.00 }")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }
}
