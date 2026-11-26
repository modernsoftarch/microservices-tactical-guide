package com.tmsa.brujula.recaudo.dominio;

/** El proveedor no procesó la operación; se puede reintentar sin riesgo de doble cobro. */
public class PasarelaNoDisponibleException extends RuntimeException {

    public PasarelaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
