package com.tmsa.brujula.emision.dominio;

import java.math.BigDecimal;

/** Puerto hacia el servicio de recaudo. */
public interface Recaudo {

    /**
     * Cobra la prima. Debe ser seguro llamarlo varias veces con la misma clave.
     *
     * @throws RuntimeException si no se obtuvo respuesta definitiva (red, 5xx, 409):
     *         la saga queda pendiente y se reintentará
     */
    ResultadoCobro cobrar(String claveIdempotencia, String referenciaPago, String clienteId,
                          BigDecimal monto, String medioDePago);

    record ResultadoCobro(boolean aprobado, String referencia, String motivo) {
    }
}
