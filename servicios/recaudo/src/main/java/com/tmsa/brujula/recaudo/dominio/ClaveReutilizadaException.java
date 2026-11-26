package com.tmsa.brujula.recaudo.dominio;

/** Se reutilizó una Idempotency-Key con un cuerpo distinto al original. */
public class ClaveReutilizadaException extends RuntimeException {

    public ClaveReutilizadaException(String clave) {
        super("La Idempotency-Key " + clave + " ya se usó con una solicitud diferente");
    }
}
