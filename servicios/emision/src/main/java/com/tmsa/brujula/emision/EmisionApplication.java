package com.tmsa.brujula.emision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de emisión de pólizas de Seguros Brújula.
 *
 * <p>Orquesta la saga: CotizacionAceptada → cobro en recaudo (pivote) →
 * emisión de la póliza → PolizaEmitida. Ver docs/decisiones/ADR-002.
 */
@SpringBootApplication
public class EmisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmisionApplication.class, args);
    }
}
