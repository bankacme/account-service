# account-service

Dueño de las cuentas bancarias (pasivos): apertura, saldo y reglas de cada movimiento. Ficha
completa: `bank-docs/services/account-service.md`. Contrato: `bank-docs/contracts/account-service/`.

## Comandos
- Compilar, estilo, tests y cobertura: `.\mvnw verify` (reporte en `target/site/jacoco/index.html`)
- Arrancar (necesita `config-server` arriba): `.\mvnw spring-boot:run`

## Resincronizar el contrato
Si `bank-docs/contracts/account-service/openapi.yaml` cambia:
```powershell
.\copy-contracts.ps1 -ServiceName account-service
```
Asume que `bank-docs` es una carpeta hermana (`C:\dev\bankacme\bank-docs`); si no,
pasa `-DocsRepo <ruta>`.

## P2
- **2.1 / 2.3:** se registra en Eureka y llama a otros servicios por nombre (`LoadBalancerConfig`,
  `base-url: http://<servicio>/api/v1`).
- **2.4 VIP y PYME:** las reglas (condiciones VIP/PYME del catálogo, apertura mínima, transacciones
  libres y comisión, promedio diario) ya estaban desde P1. En P2 `CreditServiceClient` reemplaza al
  adaptador no-op de `CreditCardLookupPort`: `GET credit-service /credit-cards?customerId=&status=ACTIVE`
  con circuit breaker y timeout de 2 s (503 si no responde). Solo se consulta cuando la condición
  exige tarjeta (ahorro VIP, corriente PYME): abrir una cuenta STANDARD no depende de credit-service.
