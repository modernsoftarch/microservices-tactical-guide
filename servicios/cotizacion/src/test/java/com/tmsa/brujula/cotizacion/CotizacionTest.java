package com.tmsa.brujula.cotizacion;

import com.tmsa.brujula.cotizacion.dominio.Cotizacion;
import com.tmsa.brujula.cotizacion.dominio.EstadoCotizacion;
import com.tmsa.brujula.cotizacion.dominio.Producto;
import com.tmsa.brujula.cotizacion.dominio.Tarificador;
import com.tmsa.brujula.cotizacion.dominio.TransicionInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Transiciones de estado de la cotización. */
class CotizacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-26T15:00:00Z");

    private final Cotizacion vigente = Cotizacion.nueva(
            "CC-1032", Producto.AUTO, new BigDecimal("80000000"), new Tarificador(), AHORA);

    @Test
    void aceptar_genera_el_evento_con_los_datos_que_necesita_emision() {
        Cotizacion.Aceptacion aceptacion = vigente.aceptar("tok_visa", AHORA);

        assertThat(aceptacion.cotizacion().estado()).isEqualTo(EstadoCotizacion.ACEPTADA);
        assertThat(aceptacion.evento().cotizacionId()).isEqualTo(vigente.id());
        assertThat(aceptacion.evento().prima()).isEqualByComparingTo("2800000");
        assertThat(aceptacion.evento().medioDePago()).isEqualTo("tok_visa");
    }

    @Test
    void no_se_puede_aceptar_dos_veces() {
        Cotizacion aceptada = vigente.aceptar("tok_visa", AHORA).cotizacion();

        assertThatThrownBy(() -> aceptada.aceptar("tok_visa", AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
    }
}
