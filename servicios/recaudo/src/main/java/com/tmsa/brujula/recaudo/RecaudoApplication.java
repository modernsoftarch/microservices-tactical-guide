package com.tmsa.brujula.recaudo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de recaudo de Seguros Brújula.
 *
 * <p>Recibe solicitudes de cobro (del servicio de emisión) y las ejecuta contra
 * una pasarela de pagos. Es el paso "pivote" de la saga de emisión: una vez
 * cobrado, la saga ya no se compensa, solo avanza (ver docs/decisiones/ADR-002).
 */
@SpringBootApplication
public class RecaudoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecaudoApplication.class, args);
    }
}
