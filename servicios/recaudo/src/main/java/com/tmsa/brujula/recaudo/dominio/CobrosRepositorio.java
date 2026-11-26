package com.tmsa.brujula.recaudo.dominio;

import java.time.Instant;
import java.util.Optional;

/** Puerto de persistencia de cobros e idempotencia. */
public interface CobrosRepositorio {

    /**
     * Intenta reservar la clave de idempotencia.
     *
     * @return {@code true} si la clave es nueva y este proceso debe cobrar;
     *         {@code false} si ya existía (reintento o petición concurrente)
     */
    boolean reservar(String clave, String huella, SolicitudCobro solicitud, Instant ahora);

    Optional<RegistroCobro> buscar(String clave);

    void completar(String clave, ResultadoCobro resultado);

    /** Borra una reserva cuando sabemos con certeza que no se cobró nada. */
    void liberar(String clave);
}
