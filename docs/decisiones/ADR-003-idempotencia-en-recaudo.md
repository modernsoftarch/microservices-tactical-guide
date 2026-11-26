# ADR-003: Idempotencia del cobro con Idempotency-Key e INSERT ... ON CONFLICT

- **Estado:** aceptada
- **Capítulo del libro:** 3 (Comunicación)

## Contexto

Los reintentos son inevitables: timeouts, reinicios, mensajes duplicados. Si
el endpoint de cobro no es idempotente, cada reintento puede ser un cobro doble
al cliente.

## Decisión

- El cliente envía un header `Idempotency-Key` obligatorio.
- Recaudo reserva la clave con `INSERT ... ON CONFLICT DO NOTHING` sobre una
  PRIMARY KEY. La base de datos decide quién gana entre peticiones concurrentes;
  no usamos locks en memoria (no sirven con varias réplicas).
- Se guarda una huella SHA-256 del cuerpo: la misma clave con otro cuerpo es un
  error del cliente (422), no un reintento.
- La llamada a la pasarela ocurre **fuera** de la transacción de base de datos.
- Si la pasarela confirma que no procesó (503), la reserva se libera. Si hay
  timeout, la reserva queda `EN_PROCESO` y se resuelve con conciliación, porque
  no sabemos si se cobró.

## Consecuencias

- Un reintento devuelve exactamente la respuesta original.
- Peticiones concurrentes con la misma clave reciben 409 y reintentan con backoff.
- Hay que conciliar los cobros que quedan `EN_PROCESO` (índice parcial
  `idx_cobros_en_proceso` para encontrarlos rápido).
- La tabla `cobros` crece; en producción se define una política de retención.
