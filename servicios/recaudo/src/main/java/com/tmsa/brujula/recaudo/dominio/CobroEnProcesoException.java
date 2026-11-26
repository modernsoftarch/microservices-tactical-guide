package com.tmsa.brujula.recaudo.dominio;

/** Hay otra petición con la misma clave ejecutándose ahora mismo. Reintenta luego. */
public class CobroEnProcesoException extends RuntimeException {

    public CobroEnProcesoException(String clave) {
        super("El cobro con Idempotency-Key " + clave + " se está procesando");
    }
}
