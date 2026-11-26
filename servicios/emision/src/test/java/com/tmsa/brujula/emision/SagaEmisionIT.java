package com.tmsa.brujula.emision;

import com.tmsa.brujula.emision.aplicacion.OrquestadorEmision;
import com.tmsa.brujula.emision.aplicacion.ReintentadorDeSagas;
import com.tmsa.brujula.emision.dominio.CotizacionAceptada;
import com.tmsa.brujula.emision.dominio.EstadoSaga;
import com.tmsa.brujula.emision.dominio.Recaudo;
import com.tmsa.brujula.emision.dominio.RepositorioSagas;
import com.tmsa.brujula.emision.dominio.SagaEmision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * La saga de emisión contra PostgreSQL real, con recaudo simulado (capítulos 4 y 7).
 * Kafka y los reintentos automáticos están apagados: invocamos el orquestador directamente.
 */
@SpringBootTest(properties = {
        "brujula.kafka.consumir=false",
        "brujula.saga.reintentos-automaticos=false",
        "brujula.saga.espera-antes-de-reintentar=PT0S",
        "brujula.saga.max-intentos=3"
})
@Import(ContenedoresDePrueba.class)
class SagaEmisionIT {

    @Autowired
    OrquestadorEmision orquestador;

    @Autowired
    ReintentadorDeSagas reintentador;

    @Autowired
    RepositorioSagas sagas;

    @Autowired
    JdbcClient jdbc;

    @MockitoBean
    Recaudo recaudo;

    @Test
    void cobro_aprobado_emite_la_poliza_y_publica_el_evento() {
        cobroResponde(new Recaudo.ResultadoCobro(true, "AUT-1", null));
        CotizacionAceptada evento = evento();

        orquestador.alAceptarseCotizacion(UUID.randomUUID().toString(), evento);

        SagaEmision saga = saga(evento);
        assertThat(saga.estado()).isEqualTo(EstadoSaga.EMITIDA);
        assertThat(saga.numeroPoliza()).startsWith("POL-");
        assertThat(contar("SELECT count(*) FROM polizas WHERE cotizacion_id = :c", evento)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM outbox WHERE type = 'PolizaEmitida' AND payload->>'cotizacionId' = :c", evento))
                .isEqualTo(1);
    }

    @Test
    void un_mensaje_duplicado_no_cobra_dos_veces() {
        cobroResponde(new Recaudo.ResultadoCobro(true, "AUT-2", null));
        CotizacionAceptada evento = evento();
        String idMensaje = UUID.randomUUID().toString();

        orquestador.alAceptarseCotizacion(idMensaje, evento);
        orquestador.alAceptarseCotizacion(idMensaje, evento);

        verify(recaudo, times(1)).cobrar(anyString(), anyString(), eq(evento.clienteId()), any(), anyString());
    }

    @Test
    void cobro_rechazado_cierra_la_saga_sin_poliza() {
        cobroResponde(new Recaudo.ResultadoCobro(false, null, "Fondos insuficientes"));
        CotizacionAceptada evento = evento();

        orquestador.alAceptarseCotizacion(UUID.randomUUID().toString(), evento);

        SagaEmision saga = saga(evento);
        assertThat(saga.estado()).isEqualTo(EstadoSaga.RECHAZADA);
        assertThat(saga.motivo()).isEqualTo("Fondos insuficientes");
        assertThat(contar("SELECT count(*) FROM polizas WHERE cotizacion_id = :c", evento)).isZero();
        assertThat(contar("SELECT count(*) FROM outbox WHERE type = 'EmisionRechazada' AND aggregateid = :c", evento))
                .isEqualTo(1);
    }

    @Test
    void si_recaudo_no_responde_la_saga_queda_pendiente_y_el_reintento_usa_la_misma_clave() {
        CotizacionAceptada evento = evento();
        given(recaudo.cobrar(anyString(), anyString(), eq(evento.clienteId()), any(), anyString()))
                .willThrow(new ResourceAccessException("timeout"))
                .willReturn(new Recaudo.ResultadoCobro(true, "AUT-3", null));

        orquestador.alAceptarseCotizacion(UUID.randomUUID().toString(), evento);
        SagaEmision pendiente = saga(evento);
        assertThat(pendiente.estado()).isEqualTo(EstadoSaga.COBRO_PENDIENTE);
        assertThat(pendiente.intentos()).isEqualTo(1);

        reintentador.reintentarPendientes();

        assertThat(saga(evento).estado()).isEqualTo(EstadoSaga.EMITIDA);
        verify(recaudo, times(2)).cobrar(eq(pendiente.claveDeCobro()), anyString(), anyString(), any(), anyString());
    }

    @Test
    void al_agotar_los_reintentos_la_saga_pide_revision_humana() {
        CotizacionAceptada evento = evento();
        given(recaudo.cobrar(anyString(), anyString(), eq(evento.clienteId()), any(), anyString()))
                .willThrow(new ResourceAccessException("recaudo caído"));

        orquestador.alAceptarseCotizacion(UUID.randomUUID().toString(), evento);
        reintentador.reintentarPendientes();
        reintentador.reintentarPendientes();

        assertThat(saga(evento).estado()).isEqualTo(EstadoSaga.REQUIERE_REVISION);
    }

    private void cobroResponde(Recaudo.ResultadoCobro resultado) {
        given(recaudo.cobrar(anyString(), anyString(), anyString(), any(), anyString())).willReturn(resultado);
    }

    /** Cada prueba usa un cliente distinto para no interferir con las demás. */
    private static CotizacionAceptada evento() {
        return new CotizacionAceptada(UUID.randomUUID(), "CC-" + UUID.randomUUID().toString().substring(0, 6),
                "AUTO", new BigDecimal("80000000"), new BigDecimal("2800000"), "tok_visa", Instant.now());
    }

    private SagaEmision saga(CotizacionAceptada evento) {
        return sagas.buscarPorCotizacion(evento.cotizacionId().toString()).orElseThrow();
    }

    private int contar(String sql, CotizacionAceptada evento) {
        return jdbc.sql(sql).param("c", evento.cotizacionId().toString()).query(Integer.class).single();
    }
}
