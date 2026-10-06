package com.bank.account.infrastructure.adapter.out.rest;

import com.bank.account.application.port.out.CustomerLookupPort;
import com.bank.account.domain.exception.DownstreamServiceUnavailableException;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.CustomerType;
import com.bank.account.infrastructure.support.RxJavaReactorBridge;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerServiceClient implements CustomerLookupPort {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public CustomerServiceClient(@LoadBalanced WebClient.Builder builder,
                                  @Value("${bank.clients.customer-service.base-url}") String baseUrl,
                                  CircuitBreakerRegistry circuitBreakerRegistry,
                                  TimeLimiterRegistry timeLimiterRegistry) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("customer-service");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("customer-service");
    }

    @Override
    public Maybe<CustomerSnapshot> findById(String customerId) {
        Mono<CustomerSnapshot> call = webClient.get()
                .uri("/customers/{id}", customerId)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Mono.<CustomerResponse>empty();
                    }
                    if (response.statusCode().isError()) {
                        return response.<CustomerResponse>createError();
                    }
                    return response.bodyToMono(CustomerResponse.class);
                })
                .map(CustomerServiceClient::toSnapshot)
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(ex -> new DownstreamServiceUnavailableException("customer-service", ex));
        return RxJavaReactorBridge.toMaybe(call);
    }

    private static CustomerSnapshot toSnapshot(CustomerResponse response) {
        return new CustomerSnapshot(response.id(), response.type(), response.profile(), response.status());
    }

    private record CustomerResponse(String id, CustomerType type, CustomerProfile profile, String status) {
    }
}
