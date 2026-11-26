# ADR-001: Publicar eventos con Outbox y Debezium

- **Estado:** aceptada
- **Capítulo del libro:** 4 (Datos)

## Contexto

Cuando el cliente acepta una cotización hay que hacer dos cosas: guardar el
nuevo estado en PostgreSQL y avisarle a emisión por Kafka. Si hacemos las dos
desde el código (`repositorio.save()` y luego `kafka.send()`), cualquier falla
entre una y otra deja el sistema inconsistente: cotización aceptada sin póliza,
o evento publicado de una cotización que nunca se guardó.

## Decisión

Escribir el evento en una tabla `outbox` dentro de la **misma transacción** del
cambio de negocio. Debezium lee esa tabla desde el WAL de PostgreSQL y la
publica en Kafka con el Outbox Event Router.

## Consecuencias

- Atomicidad sin transacciones distribuidas: o se guardan ambos o ninguno.
- Entrega "al menos una vez": un evento puede llegar dos veces. Los
  consumidores deben ser idempotentes (ver `mensajes_procesados` en emisión).
- Una pieza más que operar (Kafka Connect + Debezium) y vigilar (lag del conector,
  slots de replicación que retienen WAL si el conector se cae).
- El outbox crece: hay que limpiarlo con un job periódico.

## Alternativas descartadas

- **Publicar desde el código después del commit:** se pierde el evento si el
  proceso muere justo después del commit.
- **Polling del outbox con un scheduler propio:** válido para volúmenes bajos y
  equipos sin Kafka Connect. Más simple de operar, pero agrega latencia y carga
  de consultas sobre la base. Es la opción B razonable si Debezium no es viable.
