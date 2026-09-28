package com.bank.account.infrastructure.adapter.out.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.domain.exception.DownstreamServiceUnavailableException;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.CustomerType;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.reactivex.rxjava3.observers.TestObserver;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

class CustomerServiceClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private CustomerServiceClient client;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();
        TimeLimiterConfig tlConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))
                .cancelRunningFuture(true)
                .build();

        client = new CustomerServiceClient(
                WebClient.builder(),
                wireMock.baseUrl(),
                CircuitBreakerRegistry.of(cbConfig),
                TimeLimiterRegistry.of(tlConfig));
    }

    @Test
    void aKnownCustomerIsMappedFromTheResponseBody() throws InterruptedException {
        wireMock.stubFor(get(urlEqualTo("/customers/cust-1")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "id": "cust-1",
                          "type": "PERSONAL",
                          "profile": "STANDARD",
                          "status": "ACTIVE",
                          "name": "Ana Torres"
                        }
                        """)));

        TestObserver<CustomerSnapshot> observer = client.findById("cust-1").test();
        observer.await();

        observer.assertValue(new CustomerSnapshot("cust-1", CustomerType.PERSONAL, CustomerProfile.STANDARD,
                "ACTIVE"));
    }

    @Test
    void aFourOhFourIsEmptyNotAnError() throws InterruptedException {
        wireMock.stubFor(get(urlEqualTo("/customers/missing")).willReturn(aResponse().withStatus(404)));

        TestObserver<CustomerSnapshot> observer = client.findById("missing").test();
        observer.await();

        observer.assertComplete();
        observer.assertNoValues();
        observer.assertNoErrors();
    }

    @Test
    void aResponseSlowerThanTwoSecondsBecomesDownstreamServiceUnavailable() throws InterruptedException {
        wireMock.stubFor(get(urlEqualTo("/customers/cust-1")).willReturn(aResponse()
                .withFixedDelay(3000)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")));

        long start = System.nanoTime();
        TestObserver<CustomerSnapshot> observer = client.findById("cust-1").test();
        observer.await();
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        observer.assertError(DownstreamServiceUnavailableException.class);
        // Must have been cut around the 2 s limiter, not around WireMock's 3 s delay.
        assertThat(elapsedMs).isLessThan(2900);
    }

    @Test
    void aServerErrorBecomesDownstreamServiceUnavailable() throws InterruptedException {
        wireMock.stubFor(get(urlEqualTo("/customers/cust-1")).willReturn(aResponse().withStatus(500)));

        TestObserver<CustomerSnapshot> observer = client.findById("cust-1").test();
        observer.await();

        observer.assertError(DownstreamServiceUnavailableException.class);
    }

    @Test
    void theCircuitOpensAfterRepeatedTimeouts() throws InterruptedException {
        wireMock.stubFor(get(urlEqualTo("/customers/cust-1")).willReturn(aResponse().withFixedDelay(3000)));

        // minimumNumberOfCalls = 4: after these, the breaker has enough data to decide.
        for (int i = 0; i < 4; i++) {
            client.findById("cust-1").test().await();
        }

        TestObserver<CustomerSnapshot> observer = client.findById("cust-1").test();
        observer.await();
        observer.assertError(DownstreamServiceUnavailableException.class);
    }
}
