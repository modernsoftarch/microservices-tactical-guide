-- Esquema del servicio de recaudo.

-- Una fila por Idempotency-Key. La PRIMARY KEY es lo que garantiza que
-- dos peticiones con la misma clave no cobren dos veces.
CREATE TABLE cobros (
    clave_idempotencia   VARCHAR(128)  PRIMARY KEY,
    huella               CHAR(64)      NOT NULL,
    referencia_pago      VARCHAR(64)   NOT NULL,
    cliente_id           VARCHAR(64)   NOT NULL,
    monto                NUMERIC(18,0) NOT NULL,
    estado               VARCHAR(16)   NOT NULL,
    referencia_pasarela  VARCHAR(64),
    motivo               VARCHAR(256),
    creado_en            TIMESTAMPTZ   NOT NULL
);

-- Para conciliación: encontrar cobros que se quedaron EN_PROCESO.
CREATE INDEX idx_cobros_en_proceso ON cobros (creado_en) WHERE estado = 'EN_PROCESO';
