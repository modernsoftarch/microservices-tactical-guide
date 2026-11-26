package com.tmsa.brujula.emision.aplicacion;

import com.tmsa.brujula.emision.dominio.CotizacionAceptada;
import com.tmsa.brujula.emision.dominio.EstadoSaga;
import com.tmsa.brujula.emision.dominio.Eventos.EmisionRechazada;
import com.tmsa.brujula.emision.dominio.Eventos.PolizaEmitida;
import com.tmsa.brujula.emision.dominio.Outbox;
import com.tmsa.brujula.emision.dominio.Recaudo;
import com.tmsa.brujula.emision.dominio.RepositorioPolizas;
import com.tmsa.brujula.emision.dominio.RepositorioSagas;
import com.tmsa.brujula.emision.dominio.SagaEmision;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Orquestador de la saga de emisión (capítulo 4).
 *
 * <p>Tres transacciones cortas en lugar de una larga:
 * <ol>
 *   <li><b>Recibir</b>: deduplicar el mensaje y crear la saga (misma transacción).</li>
 *   <li><b>Cobrar</b>: llamada HTTP a recaudo, FUERA de cualquier transacción.
 *       La clave de idempotencia es el id de la saga.</li>
 *   <li><b>Cerrar</b>: emitir la póliza (o rechazar) y publicar el evento por outbox.</li>
 * </ol>
 *
 * <p>Si el proceso muere entre 2 y 3, la saga queda COBRO_PENDIENTE y
 * {@link ReintentadorDeSagas} la retoma. Repetir el cobro es seguro porque
 * recaudo devuelve el mismo resultado para la misma clave.
 *
 * <p>Usamos Micrometer directamente: es una decisión pragmática; una
 * abstracción propia para métricas no paga su costo en un servicio de este tamaño.
 */
@Service
public class OrquestadorEmision {

    private static final Logger log = LoggerFactory.getLogger(OrquestadorEmision.class);
    static final String TIPO_AGREGADO = "poliza";

    private final RepositorioSagas sagas;
    private final RepositorioPolizas polizas;
    private final Outbox outbox;
    private final Recaudo recaudo;
    private final TransactionTemplate tx;
    private final Clock reloj;
    private final MeterRegistry metricas;
    private final int maxIntentos;

    public OrquestadorEmision(RepositorioSagas sagas, RepositorioPolizas polizas, Outbox outbox, Recaudo recaudo,
                              TransactionTemplate tx, Clock reloj, MeterRegistry metricas,
                              @Value("${brujula.saga.max-intentos:5}") int maxIntentos) {
        this.sagas = sagas;
        this.polizas = polizas;
        this.outbox = outbox;
        this.recaudo = recaudo;
        this.tx = tx;
        this.reloj = reloj;
        this.metricas = metricas;
        this.maxIntentos = maxIntentos;
    }

    /** Paso 1. Lo invoca el consumidor de Kafka. */
    public void alAceptarseCotizacion(String idMensaje, CotizacionAceptada evento) {
        Optional<SagaEmision> saga = tx.execute(estado -> {
            if (!sagas.registrarMensaje(idMensaje)) {
                log.info("Mensaje {} duplicado; se ignora", idMensaje);
                return Optional.<SagaEmision>empty();
            }
            return Optional.of(sagas.crearSiNoExiste(SagaEmision.iniciar(evento)));
        });
        if (saga != null) {
            saga.ifPresent(this::avanzar);
        }
    }

    /** Pasos 2 y 3. También lo usa el reintentador. */
    public void avanzar(SagaEmision saga) {
        if (saga.estado().esFinal()) {
            return;
        }

        Recaudo.ResultadoCobro cobro;
        try {
            cobro = Objects.requireNonNull(recaudo.cobrar(saga.claveDeCobro(), saga.id().toString(),
                    saga.clienteId(), saga.prima(), saga.medioDePago()), "recaudo no devolvió resultado");
        } catch (RuntimeException e) {
            registrarFallo(saga, e);
            return;
        }

        Instant ahora = reloj.instant();
        if (cobro.aprobado()) {
            emitir(saga, cobro, ahora);
        } else {
            rechazar(saga, cobro.motivo(), ahora);
        }
    }

    private void emitir(SagaEmision saga, Recaudo.ResultadoCobro cobro, Instant ahora) {
        String numero = saga.numeroDePolizaAsignado();
        tx.executeWithoutResult(estado -> {
            if (!sagas.transicionar(saga.id(), EstadoSaga.COBRO_PENDIENTE, EstadoSaga.EMITIDA, numero, null)) {
                return; // otro proceso ya la cerró
            }
            polizas.guardar(numero, saga, ahora);
            outbox.registrar(TIPO_AGREGADO, numero, PolizaEmitida.TIPO, new PolizaEmitida(
                    numero, saga.cotizacionId(), saga.clienteId(), saga.producto(), saga.prima(),
                    cobro.referencia(), ahora));
            contar("emitida");
            log.info("Póliza {} emitida para la cotización {}", numero, saga.cotizacionId());
        });
    }

    private void rechazar(SagaEmision saga, String motivo, Instant ahora) {
        tx.executeWithoutResult(estado -> {
            if (!sagas.transicionar(saga.id(), EstadoSaga.COBRO_PENDIENTE, EstadoSaga.RECHAZADA, null, motivo)) {
                return;
            }
            outbox.registrar(TIPO_AGREGADO, saga.cotizacionId(), EmisionRechazada.TIPO,
                    new EmisionRechazada(saga.cotizacionId(), saga.clienteId(), motivo, ahora));
            contar("rechazada");
            log.info("Emisión rechazada para la cotización {}: {}", saga.cotizacionId(), motivo);
        });
    }

    private void registrarFallo(SagaEmision saga, RuntimeException e) {
        log.warn("No se pudo cobrar la saga {} (intento {}): {}", saga.id(), saga.intentos() + 1, e.getMessage());
        tx.executeWithoutResult(estado -> {
            sagas.registrarIntentoFallido(saga.id(), e.getClass().getSimpleName() + ": " + e.getMessage());
            if (saga.intentos() + 1 >= maxIntentos
                    && sagas.transicionar(saga.id(), EstadoSaga.COBRO_PENDIENTE, EstadoSaga.REQUIERE_REVISION,
                    null, "Reintentos agotados")) {
                contar("requiere_revision");
                log.error("Saga {} requiere revisión manual: reintentos agotados", saga.id());
            }
        });
    }

    private void contar(String resultado) {
        metricas.counter("brujula.emision.sagas", "resultado", resultado).increment();
    }
}
