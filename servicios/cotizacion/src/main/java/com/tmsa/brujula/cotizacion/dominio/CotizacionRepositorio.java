package com.tmsa.brujula.cotizacion.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de cotizaciones. La implementación vive en infraestructura. */
public interface CotizacionRepositorio {

    void guardar(Cotizacion cotizacion);

    void actualizarEstado(Cotizacion cotizacion);

    Optional<Cotizacion> buscar(UUID id);
}
