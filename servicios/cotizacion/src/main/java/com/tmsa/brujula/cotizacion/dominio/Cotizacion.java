package com.tmsa.brujula.cotizacion.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Cotización de una póliza. Es inmutable: cada cambio de estado devuelve una
 * nueva instancia, lo que hace explícitas las transiciones permitidas.
 *
 * @param id              identificador de la cotización
 * @param clienteId       identificador del cliente (viene del sistema de clientes)
 * @param producto        producto cotizado
 * @param valorAsegurado  suma asegurada en COP
 * @param prima           prima anual calculada en COP
 * @param estado          estado actual
 * @param creadaEn        instante de creación
 */
public record Cotizacion(
        UUID id,
        String clienteId,
        Producto producto,
        BigDecimal valorAsegurado,
        BigDecimal prima,
        EstadoCotizacion estado,
        Instant creadaEn) {

    public Cotizacion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(producto, "producto");
        Objects.requireNonNull(valorAsegurado, "valorAsegurado");
        Objects.requireNonNull(prima, "prima");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(creadaEn, "creadaEn");
    }

    /** Crea una cotización vigente calculando la prima con el tarificador. */
    public static Cotizacion nueva(String clienteId, Producto producto, BigDecimal valorAsegurado,
                                   Tarificador tarificador, Instant ahora) {
        BigDecimal prima = tarificador.calcularPrima(producto, valorAsegurado);
        return new Cotizacion(UUID.randomUUID(), clienteId, producto, valorAsegurado, prima,
                EstadoCotizacion.VIGENTE, ahora);
    }

    /**
     * Acepta la cotización. Solo una cotización vigente puede aceptarse.
     *
     * @param medioDePago token del medio de pago con el que el cliente pagará la prima
     * @return la cotización aceptada y el evento que el resto del sistema debe conocer
     */
    public Aceptacion aceptar(String medioDePago, Instant ahora) {
        Objects.requireNonNull(medioDePago, "medioDePago");
        if (estado != EstadoCotizacion.VIGENTE) {
            throw new TransicionInvalidaException(id, estado, EstadoCotizacion.ACEPTADA);
        }
        Cotizacion aceptada = new Cotizacion(id, clienteId, producto, valorAsegurado, prima,
                EstadoCotizacion.ACEPTADA, creadaEn);
        CotizacionAceptada evento = new CotizacionAceptada(
                id, clienteId, producto.name(), valorAsegurado, prima, medioDePago, ahora);
        return new Aceptacion(aceptada, evento);
    }

    /** Resultado de aceptar: el nuevo estado y el evento de dominio asociado. */
    public record Aceptacion(Cotizacion cotizacion, CotizacionAceptada evento) {
    }
}
