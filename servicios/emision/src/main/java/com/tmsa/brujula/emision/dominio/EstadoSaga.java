package com.tmsa.brujula.emision.dominio;

/**
 * Estados de la saga de emisión.
 *
 * <pre>
 *                 ┌── cobro aprobado ──► EMITIDA
 * COBRO_PENDIENTE ┼── cobro rechazado ─► RECHAZADA
 *                 └── reintentos agotados ─► REQUIERE_REVISION (alerta a un humano)
 * </pre>
 *
 * El cobro es el paso pivote: después de cobrar, la saga solo avanza.
 */
public enum EstadoSaga {
    COBRO_PENDIENTE,
    EMITIDA,
    RECHAZADA,
    REQUIERE_REVISION;

    public boolean esFinal() {
        return this != COBRO_PENDIENTE;
    }
}
