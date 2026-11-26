#!/usr/bin/env bash
# Recorre el flujo completo contra el entorno de docker compose:
#   1. crea una cotización   2. la acepta   3. espera a que emisión emita la póliza
#
# Uso:  ./scripts/demo.sh [token_medio_de_pago]
#   tok_visa (por defecto)        → póliza EMITIDA
#   tok_fondos_insuficientes      → emisión RECHAZADA
# Requiere curl y jq.
set -euo pipefail

TOKEN="${1:-tok_visa}"
COTIZACION=http://localhost:8081/api/v1/cotizaciones
EMISION=http://localhost:8082/api/v1/emisiones

echo "1) Cotizando un seguro de auto por 80.000.000 COP..."
ID=$(curl -sf -X POST "$COTIZACION" -H 'Content-Type: application/json' \
  -d '{"clienteId":"CC-1032","producto":"AUTO","valorAsegurado":80000000}' | jq -r .id)
echo "   cotización $ID"

echo "2) Aceptando con el medio de pago $TOKEN..."
curl -sf -X POST "$COTIZACION/$ID/aceptacion" -H 'Content-Type: application/json' \
  -d "{\"medioDePago\":\"$TOKEN\"}" | jq -c '{estado, prima}'

echo "3) Esperando la emisión (outbox → Debezium → Kafka → saga → recaudo)..."
for _ in $(seq 1 30); do
  RESPUESTA=$(curl -s "$EMISION/$ID" || true)
  ESTADO=$(echo "$RESPUESTA" | jq -r '.estado // empty' 2>/dev/null || true)
  if [[ "$ESTADO" == "EMITIDA" || "$ESTADO" == "RECHAZADA" || "$ESTADO" == "REQUIERE_REVISION" ]]; then
    echo "$RESPUESTA" | jq .
    exit 0
  fi
  sleep 2
done

echo "La emisión no terminó en 60 s. Revisa: docker compose logs emision connect registrar-conectores" >&2
exit 1
