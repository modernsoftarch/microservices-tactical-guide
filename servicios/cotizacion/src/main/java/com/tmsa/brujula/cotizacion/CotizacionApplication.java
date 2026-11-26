package com.tmsa.brujula.cotizacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de cotización de Seguros Brújula.
 *
 * <p>Organización del código (hexagonal, versión pragmática):
 * <ul>
 *   <li>{@code dominio}: reglas del negocio en Java puro, sin Spring.</li>
 *   <li>{@code aplicacion}: casos de uso; define límites transaccionales.</li>
 *   <li>{@code infraestructura}: adaptadores (HTTP, JDBC, outbox).</li>
 * </ul>
 * Las reglas de dependencia entre capas se verifican con ArchUnit en
 * {@code ReglasDeArquitecturaTest}.
 */
@SpringBootApplication
public class CotizacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CotizacionApplication.class, args);
    }
}
