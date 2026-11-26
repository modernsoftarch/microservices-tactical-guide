package com.tmsa.brujula.recaudo;

import com.tmsa.brujula.recaudo.dominio.SolicitudCobro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudCobroTest {

    @Test
    void la_huella_no_cambia_por_la_escala_del_monto() {
        var a = new SolicitudCobro("saga-1", "CC-1", new BigDecimal("2800000"), "tok_visa");
        var b = new SolicitudCobro("saga-1", "CC-1", new BigDecimal("2800000.00"), "tok_visa");

        assertThat(a.huella()).isEqualTo(b.huella());
    }

    @Test
    void la_huella_cambia_si_cambia_el_monto() {
        var a = new SolicitudCobro("saga-1", "CC-1", new BigDecimal("2800000"), "tok_visa");
        var b = new SolicitudCobro("saga-1", "CC-1", new BigDecimal("2800001"), "tok_visa");

        assertThat(a.huella()).isNotEqualTo(b.huella());
    }
}
