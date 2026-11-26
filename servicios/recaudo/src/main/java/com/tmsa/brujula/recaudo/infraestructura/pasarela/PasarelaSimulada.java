package com.tmsa.brujula.recaudo.infraestructura.pasarela;

import com.tmsa.brujula.recaudo.dominio.PasarelaDePagos;
import com.tmsa.brujula.recaudo.dominio.PasarelaNoDisponibleException;
import com.tmsa.brujula.recaudo.dominio.ResultadoCobro;
import com.tmsa.brujula.recaudo.dominio.SolicitudCobro;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptador de la pasarela de pagos (capa anticorrupción, capítulo 3).
 *
 * <p>Simula un proveedor que responde con códigos propios ("00", "51", "05"...).
 * Esos códigos NO salen de esta clase: se traducen a {@link ResultadoCobro} con
 * motivos de negocio. Si mañana cambias de proveedor, solo cambia este adaptador.
 *
 * <p>Tokens de prueba:
 * <ul>
 *   <li>{@code tok_fondos_insuficientes}: rechazo "51"</li>
 *   <li>{@code tok_bloqueada}: rechazo "05"</li>
 *   <li>{@code tok_caida}: el proveedor responde 503 (no procesó)</li>
 *   <li>cualquier otro: aprobado</li>
 * </ul>
 */
@Component
public class PasarelaSimulada implements PasarelaDePagos {

    private static final Map<String, String> MOTIVOS = Map.of(
            "51", "Fondos insuficientes",
            "05", "Medio de pago bloqueado por el emisor");

    private final AtomicInteger cobrosEjecutados = new AtomicInteger();

    @Override
    public ResultadoCobro cobrar(SolicitudCobro solicitud) {
        RespuestaProveedor respuesta = llamarProveedor(solicitud.medioDePago());
        if (respuesta.httpStatus() == 503) {
            throw new PasarelaNoDisponibleException("Pasarela no disponible, no se procesó el cobro");
        }
        cobrosEjecutados.incrementAndGet();
        return "00".equals(respuesta.codigo())
                ? ResultadoCobro.aprobado(respuesta.autorizacion())
                : ResultadoCobro.rechazado(MOTIVOS.getOrDefault(respuesta.codigo(), "Rechazado por la pasarela"));
    }

    /** Cuántas veces "llegó" un cobro al proveedor. Lo usan las pruebas para detectar dobles cobros. */
    public int cobrosEjecutados() {
        return cobrosEjecutados.get();
    }

    /** Lo que devolvería el proveedor real; su forma es ajena a nuestro dominio. */
    private RespuestaProveedor llamarProveedor(String token) {
        return switch (token) {
            case "tok_fondos_insuficientes" -> new RespuestaProveedor(200, "51", null);
            case "tok_bloqueada" -> new RespuestaProveedor(200, "05", null);
            case "tok_caida" -> new RespuestaProveedor(503, null, null);
            default -> new RespuestaProveedor(200, "00", "AUT-" + UUID.randomUUID().toString().substring(0, 8));
        };
    }

    private record RespuestaProveedor(int httpStatus, String codigo, String autorizacion) {
    }
}
