package com.tmsa.brujula.emision.aplicacion;

import com.tmsa.brujula.emision.dominio.RepositorioSagas;
import com.tmsa.brujula.emision.dominio.SagaEmision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Retoma sagas que se quedaron en COBRO_PENDIENTE (caída de recaudo, reinicio
 * del pod, timeout...). Es la "red" que hace que la saga termine siempre.
 *
 * <p>La programación periódica está en
 * {@code infraestructura.ProgramacionDeReintentos}; aquí solo está la lógica,
 * así las pruebas la invocan directamente.
 */
@Component
public class ReintentadorDeSagas {

    private final RepositorioSagas sagas;
    private final OrquestadorEmision orquestador;
    private final Clock reloj;
    private final Duration espera;

    public ReintentadorDeSagas(RepositorioSagas sagas, OrquestadorEmision orquestador, Clock reloj,
                               @Value("${brujula.saga.espera-antes-de-reintentar:PT30S}") Duration espera) {
        this.sagas = sagas;
        this.orquestador = orquestador;
        this.reloj = reloj;
        this.espera = espera;
    }

    /** @return cuántas sagas intentó avanzar */
    public int reintentarPendientes() {
        List<SagaEmision> pendientes = sagas.pendientes(reloj.instant().minus(espera), 50);
        pendientes.forEach(orquestador::avanzar);
        return pendientes.size();
    }
}
