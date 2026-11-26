package com.tmsa.brujula.emision.dominio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de la saga y de la deduplicación de mensajes. */
public interface RepositorioSagas {

    /**
     * Registra el id de un mensaje consumido.
     *
     * @return {@code false} si ya se había procesado (duplicado de Kafka)
     */
    boolean registrarMensaje(String idMensaje);

    /** Crea la saga si no existe una para esa cotización; devuelve la que quede guardada. */
    SagaEmision crearSiNoExiste(SagaEmision saga);

    Optional<SagaEmision> buscarPorCotizacion(String cotizacionId);

    /**
     * Cambia de estado solo si la saga sigue en {@code desde}. Es un "compare and set":
     * si dos procesos intentan avanzar la misma saga, solo uno gana.
     *
     * @return {@code true} si este proceso hizo la transición
     */
    boolean transicionar(UUID id, EstadoSaga desde, EstadoSaga hacia, String numeroPoliza, String motivo);

    void registrarIntentoFallido(UUID id, String error);

    /** Sagas pendientes sin movimiento desde {@code antesDe}. */
    List<SagaEmision> pendientes(Instant antesDe, int limite);
}
