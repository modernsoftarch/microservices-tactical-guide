package com.tmsa.brujula.cotizacion.infraestructura.web;

import com.tmsa.brujula.cotizacion.aplicacion.ServicioCotizacion;
import com.tmsa.brujula.cotizacion.dominio.Cotizacion;
import com.tmsa.brujula.cotizacion.dominio.Producto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * API HTTP de cotización, versión 1 (versionado en la ruta, capítulo 3).
 *
 * <pre>
 * POST /api/v1/cotizaciones                 crea una cotización
 * GET  /api/v1/cotizaciones/{id}            consulta
 * POST /api/v1/cotizaciones/{id}/aceptacion acepta y dispara la emisión  {"medioDePago": "tok_..."}
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/cotizaciones")
class CotizacionController {

    private final ServicioCotizacion servicio;

    CotizacionController(ServicioCotizacion servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    ResponseEntity<CotizacionDto> cotizar(@Valid @RequestBody SolicitudCotizacion solicitud) {
        Cotizacion c = servicio.cotizar(solicitud.clienteId(), solicitud.producto(), solicitud.valorAsegurado());
        return ResponseEntity.created(URI.create("/api/v1/cotizaciones/" + c.id())).body(CotizacionDto.de(c));
    }

    @GetMapping("/{id}")
    CotizacionDto consultar(@PathVariable UUID id) {
        return CotizacionDto.de(servicio.consultar(id));
    }

    @PostMapping("/{id}/aceptacion")
    CotizacionDto aceptar(@PathVariable UUID id, @Valid @RequestBody SolicitudAceptacion solicitud) {
        return CotizacionDto.de(servicio.aceptar(id, solicitud.medioDePago()));
    }

    /** El medio de pago llega tokenizado desde el front; aquí nunca vemos la tarjeta. */
    record SolicitudAceptacion(@NotBlank String medioDePago) {
    }

    record SolicitudCotizacion(
            @NotBlank String clienteId,
            @NotNull Producto producto,
            @NotNull @Positive BigDecimal valorAsegurado) {
    }

    /** DTO de salida: separa el contrato HTTP del modelo de dominio. */
    record CotizacionDto(UUID id, String clienteId, String producto, BigDecimal valorAsegurado,
                         BigDecimal prima, String estado, Instant creadaEn) {

        static CotizacionDto de(Cotizacion c) {
            return new CotizacionDto(c.id(), c.clienteId(), c.producto().name(), c.valorAsegurado(),
                    c.prima(), c.estado().name(), c.creadaEn());
        }
    }
}
