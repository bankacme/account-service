package com.bank.account.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.bank.account.application.command.ApplyMovementCommand;
import com.bank.account.application.port.in.ApplyMovementUseCase;
import com.bank.account.application.port.in.ReverseMovementUseCase;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.exception.OperationNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.Money;
import com.bank.account.domain.model.MovementResult;
import com.bank.account.domain.model.MovementType;
import com.bank.account.domain.model.ReversalResult;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import com.bank.account.infrastructure.mapper.AccountRestMapper;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(InternalMovementController.class)
@Import(AccountRestMapper.class)
class InternalMovementControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private ApplyMovementUseCase applyMovementUseCase;
    @MockitoBean
    private ReverseMovementUseCase reverseMovementUseCase;

    private final Account account = AccountFixtures.openPersonalSavings("cust-A", "00100000000001", "1000.00");

    @Test
    void applyMovementReturns200WithTheMovementResult() {
        MovementResult result = new MovementResult("op-1", account.id(), MovementType.WITHDRAWAL,
                Money.of(new BigDecimal("150.00")), Money.of(new BigDecimal("2.00")),
                Money.of(new BigDecimal("848.00")), 6);
        given(applyMovementUseCase.execute(any(ApplyMovementCommand.class))).willReturn(Single.just(result));

        client.post().uri("/api/v1/accounts/{id}/movements", account.id().value())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "operationId": "b7f0c2a4-19d2-4c0a-8d6e-3a9c1f7e5b20-OUT",
                          "type": "WITHDRAWAL",
                          "amount": 150.00
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.newBalance").isEqualTo(848.00)
                .jsonPath("$.movementNumber").isEqualTo(6);
    }

    @Test
    void applyMovementWithInsufficientFundsReturns422() {
        given(applyMovementUseCase.execute(any(ApplyMovementCommand.class))).willReturn(Single.error(
                new BusinessRuleViolationException("INSUFFICIENT_FUNDS", "not enough balance")));

        client.post().uri("/api/v1/accounts/{id}/movements", account.id().value())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "operationId": "op-nofunds",
                          "type": "WITHDRAWAL",
                          "amount": 999999.00
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void applyMovementWithAReusedOperationIdButDifferentDataReturns409() {
        given(applyMovementUseCase.execute(any(ApplyMovementCommand.class))).willReturn(Single.error(
                new BusinessRuleViolationException("OPERATION_ID_REUSED", "already used for a different amount")));

        client.post().uri("/api/v1/accounts/{id}/movements", account.id().value())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "operationId": "op-reused",
                          "type": "WITHDRAWAL",
                          "amount": 30.00
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("OPERATION_ID_REUSED");
    }

    @Test
    void reverseMovementReturns200WithTheReversalResult() {
        ReversalResult result = new ReversalResult("op-reversal", account.id(), Money.of(new BigDecimal("1000.00")));
        given(reverseMovementUseCase.execute(eq(account.id()), eq("op-reversal"))).willReturn(Single.just(result));

        client.post().uri("/api/v1/accounts/{id}/movements/{operationId}/reversal", account.id().value(),
                        "op-reversal")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.newBalance").isEqualTo(1000.00);
    }

    @Test
    void reverseMovementForAnUnknownOperationReturns404() {
        given(reverseMovementUseCase.execute(eq(account.id()), eq("op-missing")))
                .willReturn(Single.error(new OperationNotFoundException("op-missing", account.id().value())));

        client.post().uri("/api/v1/accounts/{id}/movements/{operationId}/reversal", account.id().value(),
                        "op-missing")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OPERATION_NOT_FOUND");
    }

    @Test
    void reverseMovementOfARejectedOperationReturns422() {
        given(reverseMovementUseCase.execute(eq(account.id()), eq("op-rejected"))).willReturn(Single.error(
                new BusinessRuleViolationException("OPERATION_NOT_APPLIED", "was rejected, nothing to reverse")));

        client.post().uri("/api/v1/accounts/{id}/movements/{operationId}/reversal", account.id().value(),
                        "op-rejected")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("OPERATION_NOT_APPLIED");
    }

    /** {@code operationId} is a path variable validated by the generated API's
     *  {@code @Size(min = 8, max = 64)}; too short must be a 400, not fall through as an
     *  unhandled 500 (GlobalExceptionHandler's ConstraintViolationException handler). */
    @Test
    void reverseMovementWithATooShortOperationIdReturns400ValidationError() {
        client.post().uri("/api/v1/accounts/{id}/movements/{operationId}/reversal", account.id().value(), "short")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }
}
