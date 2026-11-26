package com.tmsa.brujula.cotizacion.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de integración: el cliente aceptó la cotización.
 *
 * <p>Es el contrato que consume el servicio de emisión. Lleva los datos que
 * emisión necesita para no tener que llamar de vuelta a cotización
 * ("event-carried state transfer", capítulo 3). Cambiarlo es un cambio de
 * contrato: agrega campos, no los renombres ni los quites.
 *
 * <p>{@code medioDePago} es un token de la pasarela, nunca un número de tarjeta.
 */
public record CotizacionAceptada(
        UUID cotizacionId,
        String clienteId,
        String producto,
        BigDecimal valorAsegurado,
        BigDecimal prima,
        String medioDePago,
        Instant aceptadaEn) {

    /** Nombre del tipo de evento; viaja en el header {@code eventType} de Kafka. */
    public static final String TIPO = "CotizacionAceptada";
}
