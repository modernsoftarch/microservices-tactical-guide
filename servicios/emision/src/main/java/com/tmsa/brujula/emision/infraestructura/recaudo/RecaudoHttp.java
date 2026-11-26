package com.tmsa.brujula.emision.infraestructura.recaudo;

import com.tmsa.brujula.emision.dominio.Recaudo;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;

/**
 * Adaptador hacia recaudo con resiliencia nativa de Spring Framework 7 (capítulo 3).
 *
 * <ul>
 *   <li>{@code @Retryable}: reintenta errores de red, 503 y 409 con backoff
 *       exponencial. Es seguro SOLO porque enviamos la misma Idempotency-Key.</li>
 *   <li>{@code @ConcurrencyLimit}: máximo 20 cobros simultáneos desde esta
 *       instancia (bulkhead), para no tumbar a recaudo en un pico.</li>
 * </ul>
 * Los timeouts están en {@link ConfiguracionClienteRecaudo}: sin timeout, un
 * reintento no sirve de nada.
 */
@Component
class RecaudoHttp implements Recaudo {

    private final RecaudoCliente cliente;

    RecaudoHttp(RecaudoCliente cliente) {
        this.cliente = cliente;
    }

    @Override
    @Retryable(includes = {ResourceAccessException.class,
            HttpServerErrorException.ServiceUnavailable.class,
            HttpClientErrorException.Conflict.class},
            maxRetries = 3, delay = 200, multiplier = 2)
    @ConcurrencyLimit(20)
    public ResultadoCobro cobrar(String claveIdempotencia, String referenciaPago, String clienteId,
                                 BigDecimal monto, String medioDePago) {
        RecaudoCliente.RespuestaCobro r = cliente.cobrar(claveIdempotencia,
                new RecaudoCliente.SolicitudCobro(referenciaPago, clienteId, monto, medioDePago));
        if (r == null || r.estado() == null) {
            throw new IllegalStateException("Respuesta vacía de recaudo");
        }
        return new ResultadoCobro("APROBADO".equals(r.estado()), r.referencia(), r.motivo());
    }
}
