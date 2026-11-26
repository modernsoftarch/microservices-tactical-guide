-- Esquema del servicio de emisión.

-- Deduplicación de mensajes de Kafka (consumidor idempotente).
CREATE TABLE mensajes_procesados (
    id_mensaje    VARCHAR(128) PRIMARY KEY,
    procesado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Estado de cada saga. UNIQUE(cotizacion_id): una cotización, una saga.
CREATE TABLE sagas_emision (
    id              UUID          PRIMARY KEY,
    cotizacion_id   VARCHAR(64)   NOT NULL UNIQUE,
    cliente_id      VARCHAR(64)   NOT NULL,
    producto        VARCHAR(16)   NOT NULL,
    prima           NUMERIC(18,0) NOT NULL,
    medio_de_pago   VARCHAR(64)   NOT NULL,
    estado          VARCHAR(24)   NOT NULL,
    intentos        INT           NOT NULL DEFAULT 0,
    ultimo_error    VARCHAR(500),
    numero_poliza   VARCHAR(32),
    motivo          VARCHAR(256),
    creada_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizada_en  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_sagas_pendientes ON sagas_emision (actualizada_en) WHERE estado = 'COBRO_PENDIENTE';

CREATE TABLE polizas (
    numero         VARCHAR(32)   PRIMARY KEY,
    cotizacion_id  VARCHAR(64)   NOT NULL UNIQUE,
    cliente_id     VARCHAR(64)   NOT NULL,
    producto       VARCHAR(16)   NOT NULL,
    prima          NUMERIC(18,0) NOT NULL,
    emitida_en     TIMESTAMPTZ   NOT NULL
);

-- Outbox con las columnas del Outbox Event Router de Debezium.
CREATE TABLE outbox (
    id             UUID         PRIMARY KEY,
    aggregatetype  VARCHAR(64)  NOT NULL,
    aggregateid    VARCHAR(64)  NOT NULL,
    type           VARCHAR(64)  NOT NULL,
    payload        JSONB        NOT NULL,
    ocurrido_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
