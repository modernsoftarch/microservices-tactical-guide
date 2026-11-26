# Mapa: del libro al código

El eBook muestra fragmentos cortos que resaltan la decisión. Aquí está el
código completo, compilado y probado. Cada fila indica dónde buscar.

| Capítulo | Tema | Dónde está en el repositorio |
|---|---|---|
| 1. ¿Necesitas microservicios? | Límites claros antes de partir: dominio sin framework, reglas de dependencia verificadas | `servicios/*/src/main/java/.../dominio/`, `ReglasDeArquitecturaTest.java` (en cada servicio) |
| 2. Descomposición | Un servicio por capacidad de negocio, una base por servicio | `servicios/cotizacion`, `servicios/emision`, `servicios/recaudo`, `infra/postgres/` |
| 3. Comunicación | Versionado en la ruta (`/api/v1`) | `CotizacionController.java` |
| | Cliente HTTP declarativo con timeouts | `emision/.../infraestructura/recaudo/RecaudoCliente.java`, `ConfiguracionClienteRecaudo.java` |
| | Reintentos y bulkhead nativos de Spring 7 (`@Retryable`, `@ConcurrencyLimit`) | `emision/.../infraestructura/recaudo/RecaudoHttp.java` |
| | Idempotency-Key e `INSERT ... ON CONFLICT` | `recaudo/.../aplicacion/ServicioDeCobros.java`, `CobrosJdbc.java`, `CobroIdempotenteIT.java` |
| | Capa anticorrupción frente a la pasarela | `recaudo/.../infraestructura/pasarela/PasarelaSimulada.java` |
| | Tolerant reader (ignorar campos desconocidos) | `emision/.../mensajeria/ConsumidorCotizaciones.java`, `FlujoKafkaIT.java` |
| 4. Datos | Outbox en la misma transacción | `cotizacion/.../aplicacion/ServicioCotizacion.java`, `OutboxJdbc.java`, `AceptacionConOutboxIT.java` |
| | Debezium Outbox Event Router | `infra/debezium/` |
| | Consumidor idempotente | `emision/.../persistencia/SagasJdbc.java` (`registrarMensaje`) |
| | Saga orquestada con paso pivote | `emision/.../aplicacion/OrquestadorEmision.java`, `ReintentadorDeSagas.java`, `SagaEmisionIT.java` |
| 5. Infraestructura | Secretos fuera de Git | `infra/k8s/external-secret.yaml` |
| 6. Despliegue | Imagen multi-etapa, usuario sin privilegios | `Dockerfile` |
| | Probes, apagado ordenado, PDB | `infra/k8s/emision-deployment.yaml` |
| | Canary con análisis automático | `infra/k8s/emision-rollout.yaml` |
| | Pipeline | `.github/workflows/ci.yml` |
| 7. Pruebas y observabilidad | Testcontainers con PostgreSQL y Kafka reales | `*IT.java`, `ContenedoresDePrueba.java`, `ContenedorKafka.java` |
| | Fitness functions con ArchUnit | `ReglasDeArquitecturaTest.java` |
| | Métricas de negocio | `OrquestadorEmision.contar` → `brujula_emision_sagas_total` |
| | Alertas y collector | `infra/observabilidad/` |
| 8. Agentes de IA | Archivo de contexto para agentes de código | `AGENTS.md` |
| ADRs | Decisiones de este código | `docs/decisiones/` |

## Lo que el repositorio NO incluye (a propósito)

- **Autenticación OAuth2 / resource server** (capítulo 5): depende de tu
  proveedor de identidad. El libro muestra la configuración; aquí añadiría ruido.
- **Pruebas de contrato con Pact** (capítulo 7): se añadirán cuando las
  librerías de Pact soporten oficialmente Spring Boot 4 y Jackson 3.
- **Circuit breaker**: con un solo proveedor y reintentos acotados, los
  timeouts + `@ConcurrencyLimit` cubren el riesgo. Si recaudo llama a varias
  pasarelas, agrega Resilience4j.
