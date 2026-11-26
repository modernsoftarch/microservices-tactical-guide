# Contexto para agentes de código (Claude Code, Copilot, Cursor, Codex...)

Este archivo es el ejemplo del capítulo 8. Un agente lo lee antes de tocar el
código. Mantenlo corto, concreto y actualizado: es documentación que se ejecuta.

## Qué es este repositorio

Código de referencia del eBook "Microservicios: Guía Táctica de Implementación".
Tres servicios Spring Boot 4.1 / Java 21 de una aseguradora ficticia
(Seguros Brújula): `cotizacion`, `emision`, `recaudo`.

## Comandos

- Compilar y probar todo: `mvn verify` (necesita Docker; las pruebas `*IT` usan Testcontainers).
- Solo un servicio: `mvn verify -pl servicios/emision`.
- Entorno local: `docker compose up --build -d` y luego `./scripts/demo.sh`.

## Reglas que no se negocian

1. El paquete `dominio` no importa Spring, Jakarta ni Jackson. ArchUnit lo verifica.
2. Todo evento se publica con el outbox, en la misma transacción que el cambio.
   Nunca llames a Kafka directamente desde un caso de uso.
3. Toda llamada HTTP a otro servicio tiene timeout y, si se reintenta, lleva Idempotency-Key.
4. Las llamadas remotas van FUERA de las transacciones de base de datos.
5. Cambios de esquema solo con una nueva migración Flyway (`V2__...sql`); nunca edites una existente.
6. Contratos de eventos: agrega campos, no renombres ni elimines.
7. Nombres de dominio en español, como el resto del código.

## Antes de dar una tarea por terminada

- `mvn verify` pasa completo.
- Si tocaste un flujo, hay una prueba `*IT` que lo cubre.
- Si tomaste una decisión de arquitectura, agrega un ADR en `docs/decisiones/`.

## Lo que el agente NO debe hacer sin preguntar

- Cambiar versiones de Spring Boot, Kafka o Debezium.
- Tocar `infra/k8s/` o la configuración de los conectores de Debezium.
- Desactivar o borrar pruebas para que el build pase.
