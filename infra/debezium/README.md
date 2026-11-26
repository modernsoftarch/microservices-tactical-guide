# Conectores Debezium (Outbox Event Router)

Cada servicio que publica eventos tiene un conector que lee **solo** la tabla
`outbox` desde el WAL de PostgreSQL (replicación lógica con `pgoutput`) y la
transforma con el [Outbox Event Router](https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html):

| Columna del outbox | Qué hace Debezium con ella |
|---|---|
| `id` | header `id` del mensaje (los consumidores lo usan para deduplicar) |
| `aggregatetype` | elige el tópico: `brujula.<aggregatetype>` |
| `aggregateid` | key del mensaje → orden garantizado por agregado |
| `type` | header `eventType` |
| `payload` | valor del mensaje (JSON) |

Tópicos resultantes:

- `brujula.cotizacion`: `CotizacionAceptada` (lo consume emisión)
- `brujula.poliza`: `PolizaEmitida`, `EmisionRechazada`

`docker compose` registra los conectores automáticamente (servicio
`registrar-conectores`). Para hacerlo a mano:

```bash
curl -X PUT -H "Content-Type: application/json" \
  --data @infra/debezium/outbox-cotizacion.json \
  http://localhost:18083/connectors/outbox-cotizacion/config

curl -s http://localhost:18083/connectors/outbox-cotizacion/status   # debe decir RUNNING
```

**Limpieza del outbox.** La tabla crece sin límite. Debezium lee el WAL, no la
tabla, así que puedes borrar filas viejas con un job diario
(`DELETE FROM outbox WHERE ocurrido_en < now() - interval '7 days'`) sin perder eventos.
