package com.tmsa.brujula.emision.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Estado persistido de una saga de emisión.
 *
 * <p>El {@code id} de la saga se usa como Idempotency-Key ante recaudo: por
 * eso podemos reintentar el cobro cuantas veces haga falta sin cobrar doble.
 */
public record SagaEmision(
        UUID id,
        String cotizacionId,
        String clienteId,
        String producto,
        BigDecimal prima,
        String medioDePago,
        EstadoSaga estado,
        int intentos,
        String numeroPoliza,
        String motivo) {

    public static SagaEmision iniciar(CotizacionAceptada evento) {
        return new SagaEmision(UUID.randomUUID(), evento.cotizacionId().toString(), evento.clienteId(),
                evento.producto(), evento.prima(), evento.medioDePago(), EstadoSaga.COBRO_PENDIENTE,
                0, null, null);
    }

    /** Clave de idempotencia para el cobro: estable durante toda la vida de la saga. */
    public String claveDeCobro() {
        return id.toString();
    }

    /** Número de póliza derivado de la saga: determinista, así un reintento no crea otro. */
    public String numeroDePolizaAsignado() {
        return "POL-" + id.toString().substring(0, 8).toUpperCase();
    }
}
