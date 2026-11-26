package com.tmsa.brujula.recaudo.dominio;

/** Lo que guardamos por cada Idempotency-Key. */
public record RegistroCobro(String clave, String huella, EstadoCobro estado, String referencia, String motivo) {

    public ResultadoCobro comoResultado() {
        return new ResultadoCobro(estado, referencia, motivo);
    }
}
