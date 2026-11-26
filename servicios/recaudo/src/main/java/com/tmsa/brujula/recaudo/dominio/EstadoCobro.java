package com.tmsa.brujula.recaudo.dominio;

/**
 * EN_PROCESO: la clave quedó reservada y el cobro se está ejecutando.
 * APROBADO / RECHAZADO: resultado definitivo; se devuelve igual en cada reintento.
 */
public enum EstadoCobro {
    EN_PROCESO,
    APROBADO,
    RECHAZADO
}
