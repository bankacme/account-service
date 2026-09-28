# Secuencia — Abrir cuenta (`POST /accounts`)

Implementado en `OpenAccountUseCaseImpl` (R3) + `AccountController` / `GlobalExceptionHandler`
(R5) + `AccountOpeningPolicy` (R2, cadena de 9 reglas, data-model.md 3.4) + `AccountProductSeeder`
(R6, garantiza que todo `<TIPO>_STANDARD` exista) + `CustomerServiceClient` (R7, REST + circuit
breaker a `customer-service`). Es P1/P2: en P3, `CustomerLookupPort` y `CreditCardLookupPort` se
reimplementan contra read models locales (`customer_snapshots`, `credit_card_snapshots`,
alimentados por Kafka) en vez de llamar por REST — el puerto no cambia, solo el adaptador.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente HTTP (ADMIN/TELLER/CUSTOMER)
    participant Ctrl as AccountController
    participant UC as OpenAccountUseCaseImpl
    participant CS as CustomerLookupPort<br/>(CustomerServiceClient, REST+circuit breaker)
    participant OD as OverdueDebtPort (no-op P1)
    participant AR as AccountRepositoryPort (Mongo)
    participant CC as CreditCardLookupPort (no-op P1)
    participant PR as AccountProductRepositoryPort
    participant POL as AccountOpeningPolicy
    participant Dom as Account (aggregate)
    participant Evt as AccountEventPublisherPort
    participant EH as GlobalExceptionHandler

    C->>Ctrl: POST /api/v1/accounts (OpenAccountRequest)
    Ctrl->>UC: execute(OpenAccountCommand)

    UC->>CS: findById(customerId)
    alt Cliente no existe
        CS-->>UC: vacío
        UC-->>Ctrl: error CustomerNotFoundException
        Ctrl-->>EH: CustomerNotFoundException
        EH-->>C: 404 CUSTOMER_NOT_FOUND
    else Cliente existe
        CS-->>UC: CustomerSnapshot (type, profile, status)
        UC->>OD: hasOverdueDebt(customerId)
        OD-->>UC: false (P1)
        UC->>AR: countActiveByCustomerAndType(customerId, type)
        AR-->>UC: activeCount
        UC->>CC: hasActiveCreditCard(customerId)
        CC-->>UC: false (P1: credit-service no existe todavía, tarea 2.4)
        UC->>PR: findByTypeAndProfile(type, profile)
        PR-->>UC: AccountProduct (o fallback a <TIPO>_STANDARD)
        UC->>POL: validate(context)
        Note over POL: 9 validadores en cadena: cliente existe -> activo -> deuda vencida -><br/>tipo permitido -> límite de cuentas -> titulares/firmantes -><br/>día de plazo fijo -> tarjeta de crédito -> monto de apertura.<br/>La primera regla que falla gana.
        alt Alguna regla rechaza
            POL-->>UC: throws BusinessRuleViolationException
            UC-->>Ctrl: error BusinessRuleViolationException
            Ctrl-->>EH: BusinessRuleViolationException
            EH-->>C: 422 (code de la regla, p.ej. CREDIT_CARD_REQUIRED)
        else Todo OK
            POL-->>UC: OK
            UC->>Dom: Account.open(type, customerId, ..., conditions, holders, signers, ...)
            Dom-->>UC: Account (ACTIVE, número generado, saldo = openingAmount)
            UC->>AR: save(account)
            AR-->>UC: Account guardado
            UC->>Evt: publish(AccountCreated.from(saved))
            Note over Evt: No-op hasta P3 (Kafka real)
            Evt-->>UC: Completable complete
            UC-->>Ctrl: Account
            Ctrl-->>C: 201 Created (AccountDto)
        end
    end
```

## Notas

- `resolveConditions` (dentro de `OpenAccountUseCaseImpl`) busca primero por `(type, profile)` del
  cliente y si no hay entrada cae al `<TIPO>_STANDARD` del mismo tipo — el contrato lo pide así
  ("si no existe la combinación, se usa STANDARD del mismo tipo") y `AccountProductSeeder` es lo que
  garantiza que ese fallback siempre exista, incluso en una base recién creada.
- `CustomerLookupPort`, `OverdueDebtPort` y `CreditCardLookupPort` se consultan **antes** que
  `AccountOpeningPolicy`: la política es puro dominio (no llama puertos, ver su propio Javadoc), así
  que el caso de uso arma todo el contexto (`OpeningValidationContext`) primero y se lo pasa ya
  resuelto.
- Un timeout o el circuit breaker abierto en `CustomerServiceClient` no cae en esta rama de "cliente
  no existe": se traduce en `DownstreamServiceUnavailableException` → 503 `SERVICE_UNAVAILABLE`
  (ver `GlobalExceptionHandler`), un camino de error distinto al dibujado arriba.
