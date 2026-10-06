package com.bank.account.infrastructure.adapter.out.rest;

import com.bank.account.application.port.out.CreditCardLookupPort;
import com.bank.account.domain.exception.DownstreamServiceUnavailableException;
import com.bank.account.infrastructure.support.RxJavaReactorBridge;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.reactivex.rxjava3.core.Single;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * P2 (2.4): regla 7, "VIP ahorro y PYME corriente exigen una tarjeta de crédito ACTIVE". Consulta
 * {@code GET /credit-cards?customerId={id}&status=ACTIVE} de credit-service (data-model 3.2) y
 * responde si la lista trae alguna. Una tarjeta OVERDUE o CLOSED no cuenta.
 *
 * <p>Circuit breaker y timeout de 2 s (instancia "credit-service"); cualquier fallo es
 * {@link DownstreamServiceUnavailableException} → 503. En P3 lo reemplaza un read model.
 */
@Component
public class CreditServiceClient implements CreditCardLookupPort {

    private static final String ACTIVE = "ACTIVE";

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public CreditServiceClient(@LoadBalanced WebClient.Builder builder,
                               @Value("${bank.clients.credit-service.base-url}") String baseUrl,
                               CircuitBreakerRegistry circuitBreakerRegistry,
                               TimeLimiterRegistry timeLimiterRegistry) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("credit-service");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("credit-service");
    }

    @Override
    public Single<Boolean> hasActiveCreditCard(String customerId) {
        Mono<Boolean> call = webClient.get()
                .uri(uri -> uri.path("/credit-cards")
                        .queryParam("customerId", customerId)
                        .queryParam("status", ACTIVE)
                        .build())
                .retrieve()
                .bodyToFlux(CreditCardResponse.class)
                // Defensivo: aunque el filtro lo haga credit-service, solo cuenta una ACTIVE.
                .any(card -> ACTIVE.equals(card.status()))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(ex -> new DownstreamServiceUnavailableException("credit-service", ex));
        return RxJavaReactorBridge.toSingle(call);
    }

    /** Solo lo que importa de cada tarjeta; el resto del cuerpo se ignora. */
    private record CreditCardResponse(String id, String status) {
    }
}
