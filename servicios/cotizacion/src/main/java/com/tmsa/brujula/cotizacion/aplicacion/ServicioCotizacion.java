package com.tmsa.brujula.cotizacion.aplicacion;

import com.tmsa.brujula.cotizacion.dominio.Cotizacion;
import com.tmsa.brujula.cotizacion.dominio.CotizacionAceptada;
import com.tmsa.brujula.cotizacion.dominio.CotizacionNoEncontradaException;
import com.tmsa.brujula.cotizacion.dominio.CotizacionRepositorio;
import com.tmsa.brujula.cotizacion.dominio.Outbox;
import com.tmsa.brujula.cotizacion.dominio.Producto;
import com.tmsa.brujula.cotizacion.dominio.Tarificador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Casos de uso de cotización.
 *
 * <p>Punto clave del capítulo 4: {@link #aceptar(UUID, String)} cambia el estado de la
 * cotización y registra el evento en el outbox en UNA sola transacción local.
 * No publicamos a Kafka desde aquí; eso lo hace Debezium leyendo el outbox.
 */
@Service
public class ServicioCotizacion {

    static final String TIPO_AGREGADO = "cotizacion";

    private final CotizacionRepositorio repositorio;
    private final Outbox outbox;
    private final Tarificador tarificador;
    private final Clock reloj;

    public ServicioCotizacion(CotizacionRepositorio repositorio, Outbox outbox,
                              Tarificador tarificador, Clock reloj) {
        this.repositorio = repositorio;
        this.outbox = outbox;
        this.tarificador = tarificador;
        this.reloj = reloj;
    }

    @Transactional
    public Cotizacion cotizar(String clienteId, Producto producto, BigDecimal valorAsegurado) {
        Cotizacion cotizacion = Cotizacion.nueva(clienteId, producto, valorAsegurado, tarificador, reloj.instant());
        repositorio.guardar(cotizacion);
        return cotizacion;
    }

    @Transactional
    public Cotizacion aceptar(UUID id, String medioDePago) {
        Cotizacion actual = consultar(id);
        Cotizacion.Aceptacion aceptacion = actual.aceptar(medioDePago, reloj.instant());

        repositorio.actualizarEstado(aceptacion.cotizacion());
        outbox.registrar(TIPO_AGREGADO, id.toString(), CotizacionAceptada.TIPO, aceptacion.evento());

        return aceptacion.cotizacion();
    }

    @Transactional(readOnly = true)
    public Cotizacion consultar(UUID id) {
        return repositorio.buscar(id).orElseThrow(() -> new CotizacionNoEncontradaException(id));
    }
}
