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
