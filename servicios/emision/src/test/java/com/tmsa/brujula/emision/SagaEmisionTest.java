package com.tmsa.brujula.emision;

import com.tmsa.brujula.emision.dominio.CotizacionAceptada;
import com.tmsa.brujula.emision.dominio.EstadoSaga;
import com.tmsa.brujula.emision.dominio.SagaEmision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SagaEmisionTest {

    @Test
    void una_saga_nueva_empieza_esperando_el_cobro_y_su_clave_es_estable() {
        SagaEmision saga = SagaEmision.iniciar(new CotizacionAceptada(UUID.randomUUID(), "CC-1032", "AUTO",
                new BigDecimal("80000000"), new BigDecimal("2800000"), "tok_visa", Instant.now()));

        assertThat(saga.estado()).isEqualTo(EstadoSaga.COBRO_PENDIENTE);
        assertThat(saga.claveDeCobro()).isEqualTo(saga.id().toString());
        assertThat(saga.numeroDePolizaAsignado()).startsWith("POL-").hasSize(12);
    }
}
