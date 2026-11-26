package com.tmsa.brujula.emision.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Copia LOCAL del contrato publicado por cotización.
 *
 * <p>No compartimos una librería de eventos entre servicios a propósito: una
 * librería común acopla los despliegues. Cada consumidor declara solo los
 * campos que usa y tolera campos nuevos (el JSON con campos extra se ignora).
 */
public record CotizacionAceptada(
        UUID cotizacionId,
        String clienteId,
        String producto,
        BigDecimal valorAsegurado,
        BigDecimal prima,
        String medioDePago,
        Instant aceptadaEn) {

    public static final String TIPO = "CotizacionAceptada";
}
