package com.tmsa.brujula.emision.infraestructura.recaudo;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.math.BigDecimal;

/**
 * Cliente HTTP declarativo del API de recaudo (HTTP Service Client de Spring).
 * La implementación la genera Spring a partir de esta interfaz.
 */
@HttpExchange("/api/v1/cobros")
interface RecaudoCliente {

    @PostExchange
    RespuestaCobro cobrar(@RequestHeader("Idempotency-Key") String clave, @RequestBody SolicitudCobro cuerpo);

    record SolicitudCobro(String referenciaPago, String clienteId, BigDecimal monto, String medioDePago) {
    }

    /** Solo los campos que emisión usa (tolerant reader). */
    record RespuestaCobro(String estado, String referencia, String motivo) {
    }
}
