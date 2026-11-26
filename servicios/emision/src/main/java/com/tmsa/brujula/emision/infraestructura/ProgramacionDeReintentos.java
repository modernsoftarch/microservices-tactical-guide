package com.tmsa.brujula.emision.infraestructura;

import com.tmsa.brujula.emision.aplicacion.ReintentadorDeSagas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Ejecuta el reintentador cada cierto tiempo.
 *
 * <p>Con varias réplicas, todas ejecutan esta tarea; es seguro porque las
 * transiciones de la saga son "compare and set" y el cobro es idempotente.
 * Si el volumen crece, agrega un lock distribuido (por ejemplo ShedLock).
 *
 * <p>Se desactiva con {@code brujula.saga.reintentos-automaticos=false} (pruebas).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "brujula.saga.reintentos-automaticos", havingValue = "true", matchIfMissing = true)
class ProgramacionDeReintentos {

    private final ReintentadorDeSagas reintentador;

    ProgramacionDeReintentos(ReintentadorDeSagas reintentador) {
        this.reintentador = reintentador;
    }

    @Scheduled(fixedDelayString = "${brujula.saga.reintentar-cada:PT15S}")
    void reintentar() {
        reintentador.reintentarPendientes();
    }
}
