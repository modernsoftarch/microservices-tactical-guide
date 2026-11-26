package com.tmsa.brujula.emision.infraestructura.web;

import com.tmsa.brujula.emision.dominio.RepositorioSagas;
import com.tmsa.brujula.emision.dominio.SagaEmision;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta del estado de la emisión de una cotización.
 * GET /api/v1/emisiones/{cotizacionId}
 */
@RestController
@RequestMapping("/api/v1/emisiones")
class EmisionController {

    private final RepositorioSagas sagas;

    EmisionController(RepositorioSagas sagas) {
        this.sagas = sagas;
    }

    @GetMapping("/{cotizacionId}")
    ResponseEntity<EstadoEmision> consultar(@PathVariable String cotizacionId) {
        return sagas.buscarPorCotizacion(cotizacionId)
                .map(EstadoEmision::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    record EstadoEmision(String cotizacionId, String estado, String numeroPoliza, String motivo, int intentos) {
        static EstadoEmision de(SagaEmision s) {
            return new EstadoEmision(s.cotizacionId(), s.estado().name(), s.numeroPoliza(), s.motivo(), s.intentos());
        }
    }
}
