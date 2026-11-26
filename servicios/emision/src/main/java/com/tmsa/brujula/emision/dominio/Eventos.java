package com.tmsa.brujula.emision.dominio;

import java.math.BigDecimal;
import java.time.Instant;

/** Eventos que publica emisión. */
public final class Eventos {

    private Eventos() {
    }

    public record PolizaEmitida(String numeroPoliza, String cotizacionId, String clienteId, String producto,
                                BigDecimal prima, String referenciaCobro, Instant emitidaEn) {
        public static final String TIPO = "PolizaEmitida";
    }

    public record EmisionRechazada(String cotizacionId, String clienteId, String motivo, Instant rechazadaEn) {
        public static final String TIPO = "EmisionRechazada";
    }
}
