# ADR-002: Saga orquestada para la emisión, con el cobro como paso pivote

- **Estado:** aceptada
- **Capítulo del libro:** 4 (Datos)

## Contexto

Emitir una póliza toca dos servicios con bases de datos distintas: recaudo
(cobrar la prima) y emisión (crear la póliza). No hay transacción que los cubra
a ambos. Cobrar es la operación que no se puede "deshacer" limpiamente: un
reverso existe, pero tiene costo, tarda días y genera reclamos.

## Decisión

- Una **saga orquestada** por el servicio de emisión, con su estado persistido
  en `sagas_emision`.
- El **cobro es el paso pivote**: antes de él, todo es compensable; después de
  él, la saga solo avanza (reintentos), nunca compensa.
- La Idempotency-Key del cobro es el id de la saga. Así, reintentar el cobro
  después de un timeout o un reinicio es seguro.
- Si los reintentos se agotan, la saga pasa a `REQUIERE_REVISION` y se alerta a
  una persona. No inventamos una compensación automática para un caso que el
  negocio prefiere revisar a mano.

## Consecuencias

- El estado de cada emisión es consultable (`GET /api/v1/emisiones/{cotizacionId}`)
  y auditable, algo que el área de cumplimiento valora.
- La lógica del flujo vive en un solo lugar (`OrquestadorEmision`), fácil de leer.
- Emisión depende de la disponibilidad de recaudo; lo mitigan los reintentos,
  el timeout y el reintentador periódico.

## Alternativas descartadas

- **Coreografía pura** (cada servicio reacciona a eventos del otro): para un
  flujo de dos pasos con un pivote de dinero, la lógica queda repartida y es
  difícil responder "¿en qué va la emisión de esta cotización?".
- **Cobrar después de emitir:** obligaría a anular pólizas cuando el cobro falla,
  con implicaciones regulatorias (la póliza ya existió).
