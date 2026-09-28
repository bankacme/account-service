# Secuencia — Aplicar un movimiento (`POST /accounts/{id}/movements`)

Interno (`x-gateway-internal: true`, ver el contrato): en P1/P2 lo llama `transaction-service` por
REST; en P3 el mismo caso de uso se dispara desde un comando de Kafka (`account.command`), sin
cambiar `ApplyMovementUseCaseImpl`. Implementado en `ApplyMovementUseCaseImpl` (R3) +
`InternalMovementController` / `GlobalExceptionHandler` (R5).

```mermaid
sequenceDiagram
    autonumber
    actor C as transaction-service (interno)
    participant Ctrl as InternalMovementController
    participant UC as ApplyMovementUseCaseImpl
    participant OL as OperationLogPort
    participant AR as AccountRepositoryPort
    participant Dom as Account (aggregate)
    participant UOW as UnitOfWorkPort
    participant Evt as AccountEventPublisherPort
    participant EH as GlobalExceptionHandler

    C->>Ctrl: POST /api/v1/accounts/{id}/movements (ApplyMovementRequest)
    Ctrl->>UC: execute(ApplyMovementCommand)
    UC->>OL: find(operationId)

    alt operationId ya usado
        OL-->>UC: AccountOperation guardada
        alt Cuenta/tipo/monto no coinciden
            UC-->>Ctrl: error BusinessRuleViolationException
            Ctrl-->>EH: BusinessRuleViolationException
            EH-->>C: 409 OPERATION_ID_REUSED
        else Coincide (mismo operationId, cuenta, tipo y monto)
            UC-->>Ctrl: resultado original (APPLIED o el mismo 422 si fue REJECTED)
            Ctrl-->>C: mismo código, sin volver a mover el saldo
        end
    else operationId nuevo
        OL-->>UC: vacío
        UC->>AR: findById(accountId)
        AR-->>UC: Account
        UC->>Dom: applyMovement(operationId, type, amount, date)
        Note over Dom: orden: cuenta activa -> monto > 0 -> date válida -><br/>día y tope mensual (ahorro/plazo fijo) -><br/>comisión si movementNumber > freeTransactionsLimit -> saldo suficiente
        alt Regla de negocio rechaza
            Dom-->>UC: throws BusinessRuleViolationException
            UC->>OL: save(AccountOperation REJECTED, reasonCode)
            UC->>Evt: publish(MovementRejected)
            UC-->>Ctrl: error BusinessRuleViolationException
            Ctrl-->>EH: BusinessRuleViolationException
            EH-->>C: 422 (code = reasonCode)
        else Aplicado
            Dom-->>UC: Account actualizado + MovementResult (fee, newBalance, movementNumber)
            UC->>UOW: saveAccountAndOperation(account, operación APPLIED)
            Note over UOW: transacción Mongo: accounts + account_operations juntos (atómico)
            UOW-->>UC: guardado
            UC->>Evt: publish(MovementApplied.from(result))
            Evt-->>UC: Completable complete
            UC-->>Ctrl: MovementResult
            Ctrl-->>C: 200 OK
        end
    end
```

## Notas

- La comisión se cobra **también en depósitos**: `movementNumber` cuenta todos los movimientos del
  mes (depósitos y retiros); al superar `freeTransactionsLimit` se descuenta `transactionFee` del
  saldo sin importar el tipo del movimiento que la disparó.
- Un rechazo por regla de negocio **sí** se guarda en `account_operations` (estado `REJECTED`, con
  `reasonCode`): es lo que permite que repetir el mismo `operationId` devuelva el mismo 422 en vez
  de reevaluar las reglas contra un estado de cuenta que pudo haber cambiado.
- `saveAccountAndOperation` es la única escritura atómica del flujo: si el guardado del movimiento
  fallara después de mover el saldo en memoria, ninguna de las dos colecciones queda con datos a
  medias — ver `UnitOfWorkPort` (R4, transacción de Mongo, requiere *replica set*).
