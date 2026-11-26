package com.tmsa.brujula.cotizacion.dominio;

import java.util.UUID;

/** La cotización solicitada no existe. */
public class CotizacionNoEncontradaException extends RuntimeException {

    public CotizacionNoEncontradaException(UUID id) {
        super("No existe la cotización " + id);
    }
}
