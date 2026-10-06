package com.bank.account.infrastructure.adapter.out.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.bank.account.domain.exception.DownstreamServiceUnavailableException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.reactivex.rxjava3.observers.TestObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

class CreditServiceClientTest {

    private static final String CARDS = "/credit-cards";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private CreditServiceClient client;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();
        // Mismo comportamiento que los 2 s reales, más corto para que la prueba no espere.
        TimeLimiterConfig tlConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(500))
                .cancelRunningFuture(true)
                .build();
        client = new CreditServiceClient(WebClient.builder(), wireMock.baseUrl(),
                CircuitBreakerRegistry.of(cbConfig), TimeLimiterRegistry.of(tlConfig));
    }

    private TestObserver<Boolean> lookup(String customerId) {
        return client.hasActiveCreditCard(customerId).test().awaitDone(5, TimeUnit.SECONDS);
    }

    @Test
    void anActiveCardInTheListIsTrueAndTheFiltersAreSent() {
        wireMock.stubFor(get(urlPathEqualTo(CARDS)).willReturn(okJson("""
                [ { "id": "cc-1", "customerId": "cust-V", "status": "ACTIVE", "creditLimit": 3000.00 } ]
                """)));

        lookup("cust-V").assertValue(true);

        wireMock.verify(getRequestedFor(urlPathEqualTo(CARDS))
                .withQueryParam("customerId", equalTo("cust-V"))
                .withQueryParam("status", equalTo("ACTIVE")));
    }

    @Test
    void anEmptyListIsFalse() {
        wireMock.stubFor(get(urlPathEqualTo(CARDS)).willReturn(okJson("[]")));

        lookup("cust-A").assertValue(false);
    }

    @Test
    void anOverdueOrClosedCardDoesNotCount() {
        wireMock.stubFor(get(urlPathEqualTo(CARDS)).willReturn(okJson("""
                [ { "id": "cc-1", "status": "OVERDUE" }, { "id": "cc-2", "status": "CLOSED" } ]
                """)));

        lookup("cust-V").assertValue(false);
    }

    @Test
    void aServerErrorIsDownstreamUnavailable() {
        wireMock.stubFor(get(urlPathEqualTo(CARDS)).willReturn(aResponse().withStatus(500)));

        lookup("cust-V").assertError(DownstreamServiceUnavailableException.class);
    }

    @Test
    void aSlowResponseTimesOutAsDownstreamUnavailable() {
        wireMock.stubFor(get(urlPathEqualTo(CARDS)).willReturn(okJson("[]").withFixedDelay(1500)));

        lookup("cust-V").assertError(DownstreamServiceUnavailableException.class);
    }
}
