-- Esquema del servicio de cotización.
-- Cada servicio es dueño de su base de datos (capítulo 4): nadie más escribe aquí.

CREATE TABLE cotizaciones (
    id               UUID          PRIMARY KEY,
    cliente_id       VARCHAR(64)   NOT NULL,
    producto         VARCHAR(16)   NOT NULL,
    valor_asegurado  NUMERIC(18,0) NOT NULL,
    prima            NUMERIC(18,0) NOT NULL,
    estado           VARCHAR(16)   NOT NULL,
    creada_en        TIMESTAMPTZ   NOT NULL
);

-- Tabla outbox con las columnas que espera el Outbox Event Router de Debezium.
-- Debezium la lee desde el WAL (replicación lógica), no con consultas.
CREATE TABLE outbox (
    id             UUID         PRIMARY KEY,
    aggregatetype  VARCHAR(64)  NOT NULL,
    aggregateid    VARCHAR(64)  NOT NULL,
    type           VARCHAR(64)  NOT NULL,
    payload        JSONB        NOT NULL,
    ocurrido_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
