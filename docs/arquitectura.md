# Arquitectura de Seguros Brújula (código de referencia)

Seguros Brújula es la aseguradora ficticia que acompaña el libro. Este
repositorio implementa el flujo **cotizar → aceptar → cobrar → emitir póliza**
con tres servicios.

## Vista de contenedores (C4 nivel 2)

```mermaid
flowchart LR
    canal([Canal digital / asesor]) -->|HTTP| COT[Cotización<br/>Spring Boot]
    COT -->|JDBC| DBC[(PostgreSQL<br/>cotizacion)]
    DBC -. WAL .-> DBZ[Kafka Connect<br/>Debezium Outbox]
    DBZ -->|brujula.cotizacion| K{{Kafka}}
    K -->|CotizacionAceptada| EMI[Emisión<br/>Spring Boot]
    EMI -->|JDBC| DBE[(PostgreSQL<br/>emision)]
    EMI -->|HTTP + Idempotency-Key| REC[Recaudo<br/>Spring Boot]
    REC -->|JDBC| DBR[(PostgreSQL<br/>recaudo)]
    REC -->|ACL| PAS[Pasarela de pagos<br/>simulada]
    DBE -. WAL .-> DBZ
    DBZ -->|brujula.poliza| K
```

## Secuencia del camino feliz

```mermaid
sequenceDiagram
    participant C as Cliente
    participant COT as Cotización
    participant DBZ as Debezium
    participant K as Kafka
    participant EMI as Emisión
    participant REC as Recaudo

    C->>COT: POST /cotizaciones/{id}/aceptacion
    COT->>COT: TX: estado=ACEPTADA + fila en outbox
    DBZ->>K: CotizacionAceptada (headers id, eventType)
    K->>EMI: entrega (al menos una vez)
    EMI->>EMI: TX: mensajes_procesados + saga COBRO_PENDIENTE
    EMI->>REC: POST /cobros (Idempotency-Key = id de la saga)
    REC-->>EMI: APROBADO
    EMI->>EMI: TX: póliza + saga EMITIDA + outbox PolizaEmitida
    DBZ->>K: PolizaEmitida
```

## Qué pasa cuando algo falla

| Falla | Qué hace el sistema | Dónde está el código |
|---|---|---|
| Kafka entrega el evento dos veces | `mensajes_procesados` lo descarta | `SagasJdbc.registrarMensaje` |
| Recaudo no responde | `@Retryable` reintenta 3 veces; si no, la saga queda pendiente | `RecaudoHttp` |
| Emisión se reinicia a mitad del cobro | el reintentador retoma la saga con la misma clave | `ReintentadorDeSagas` |
| Dos réplicas avanzan la misma saga | solo una gana el `UPDATE ... WHERE estado = ...` | `SagasJdbc.transicionar` |
| Reintentos agotados | saga `REQUIERE_REVISION` + alerta | `OrquestadorEmision`, `prometheus-reglas.yaml` |
| La pasarela rechaza el pago | saga `RECHAZADA` + evento `EmisionRechazada` | `OrquestadorEmision.rechazar` |
| Mensaje que no se puede procesar | 3 reintentos y luego al tópico `-dlt` | `ConfiguracionEmision` |
