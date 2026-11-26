package com.tmsa.brujula.recaudo.infraestructura.web;

import com.tmsa.brujula.recaudo.aplicacion.ServicioDeCobros;
import com.tmsa.brujula.recaudo.dominio.ResultadoCobro;
import com.tmsa.brujula.recaudo.dominio.SolicitudCobro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * POST /api/v1/cobros con el header {@code Idempotency-Key} obligatorio.
 *
 * <p>Respuestas:
 * <ul>
 *   <li>200: resultado del cobro (APROBADO o RECHAZADO). Reintentos con la misma clave devuelven lo mismo.</li>
 *   <li>409: hay otra petición con esa clave en proceso; reintenta con backoff.</li>
 *   <li>422: la clave ya se usó con otro cuerpo.</li>
 *   <li>503: la pasarela no procesó el cobro; es seguro reintentar.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/cobros")
class CobroController {

    private final ServicioDeCobros servicio;

    CobroController(ServicioDeCobros servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    ResultadoCobro cobrar(@RequestHeader("Idempotency-Key") @Size(min = 8, max = 128) String clave,
                          @Valid @RequestBody SolicitudCobroDto cuerpo) {
        return servicio.cobrar(clave, cuerpo.aDominio());
    }

    record SolicitudCobroDto(@NotBlank String referenciaPago,
                             @NotBlank String clienteId,
                             @NotNull @Positive BigDecimal monto,
                             @NotBlank String medioDePago) {

        SolicitudCobro aDominio() {
            return new SolicitudCobro(referenciaPago, clienteId, monto, medioDePago);
        }
    }
}
