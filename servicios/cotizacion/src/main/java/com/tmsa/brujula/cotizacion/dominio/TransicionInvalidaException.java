package com.tmsa.brujula.cotizacion.dominio;

import java.util.UUID;

/** Se lanza cuando se intenta un cambio de estado que el negocio no permite. */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(UUID id, EstadoCotizacion actual, EstadoCotizacion destino) {
        super("La cotización " + id + " está " + actual + " y no puede pasar a " + destino);
    }
}
