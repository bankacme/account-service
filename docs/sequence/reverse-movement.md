# Secuencia — Revertir un movimiento (`POST /accounts/{id}/movements/{operationId}/reversal`)

Interno (`x-gateway-internal: true`): lo dispara la saga de compensación de `transaction-service`
cuando la segunda pata de una transferencia falla (ver `bank-docs/flows/01-transfer.md`).
Implementado en `ReverseMovementUseCaseImpl` (R3) + `InternalMovementController` /
`GlobalExceptionHandler` (R5).

```mermaid
sequenceDiagram
    autonumber
    actor C as transaction-service (saga de compensación)
    participant Ctrl as InternalMovementController
    participant UC as ReverseMovementUseCaseImpl
    participant OL as OperationLogPort
    participant AR as AccountRepositoryPort
    participant Dom as Account (aggregate)
    participant UOW as UnitOfWorkPort
    participant Evt as AccountEventPublisherPort
    participant EH as GlobalExceptionHandler

    C->>Ctrl: POST /api/v1/accounts/{id}/movements/{operationId}/reversal
    Ctrl->>UC: execute(accountId, operationId)
    UC->>OL: find(operationId)

    alt No existe
        OL-->>UC: vacío
        UC-->>Ctrl: error OperationNotFoundException
        Ctrl-->>EH: OperationNotFoundException
        EH-->>C: 404 OPERATION_NOT_FOUND
    else Ya estaba REVERSED
        OL-->>UC: AccountOperation (REVERSED)
        UC-->>Ctrl: resultado original (idempotente)
        Ctrl-->>C: 200 OK, sin volver a mover el saldo
    else Estaba REJECTED
        OL-->>UC: AccountOperation (REJECTED)
        UC-->>Ctrl: error BusinessRuleViolationException
        Ctrl-->>EH: BusinessRuleViolationException
        EH-->>C: 422 OPERATION_NOT_APPLIED
    else Estaba APPLIED
        OL-->>UC: AccountOperation (APPLIED)
        UC->>AR: findById(accountId)
        Note over AR: Funciona aunque la cuenta esté INACTIVE<br/>(compensar es devolver dinero, no operar)
        AR-->>UC: Account
        UC->>Dom: reverseMovement(operationId, type, amount, fee, yearMonth, today)
        alt Saldo insuficiente para revertir un depósito
            Dom-->>UC: throws BusinessRuleViolationException (INSUFFICIENT_FUNDS)
            UC-->>Ctrl: error BusinessRuleViolationException
            Ctrl-->>EH: BusinessRuleViolationException
            EH-->>C: 422 INSUFFICIENT_FUNDS
        else Revertido
            Dom-->>UC: Account actualizado + ReversalResult
            UC->>UOW: saveAccountAndOperation(account, operación marcada REVERSED)
            UOW-->>UC: guardado
            UC->>Evt: publish(MovementReversed.from(result))
            Evt-->>UC: Completable complete
            UC-->>Ctrl: ReversalResult
            Ctrl-->>C: 200 OK
        end
    end
```

## Notas

- Sin cuerpo en la petición: todo sale de la `AccountOperation` guardada (monto, comisión,
  `yearMonth`), así que revertir depende solo de historial propio, nunca de lo que envíe el
  llamador.
- A diferencia de `applyMovement`, una reversa que falla (`INSUFFICIENT_FUNDS`) **no** se guarda
  como un nuevo registro: no hay nada que hacer idempotente sobre una reversa que nunca ocurrió — la
  ficha (`account-service.md` 12, pendientes) solo documenta esta forma para movimientos nuevos
  rechazados, no para reversas.
- Si la cuenta fue cerrada y la reversa falla, la saga de `transaction-service` termina en
  `COMPENSATION_FAILED` (`flows/01-transfer.md`) — `account-service` no reintenta por su cuenta.
