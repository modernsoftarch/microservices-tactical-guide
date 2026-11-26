package com.tmsa.brujula.cotizacion;

import com.tmsa.brujula.cotizacion.aplicacion.ServicioCotizacion;
import com.tmsa.brujula.cotizacion.dominio.Cotizacion;
import com.tmsa.brujula.cotizacion.dominio.Outbox;
import com.tmsa.brujula.cotizacion.dominio.Producto;
import com.tmsa.brujula.cotizacion.dominio.TransicionInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración del patrón Outbox contra PostgreSQL real.
 *
 * <p>Verifica lo que importa del capítulo 4: aceptar una cotización deja el
 * cambio de estado Y el evento en la misma transacción, y nunca un evento sin
 * su cambio de negocio.
 */
@SpringBootTest
@Import(ContenedoresDePrueba.class)
class AceptacionConOutboxIT {

    @Autowired
    ServicioCotizacion servicio;

    @Autowired
    Outbox outbox;

    @Autowired
    JdbcClient jdbc;

    @Test
    void aceptar_guarda_el_estado_y_el_evento_juntos() {
        Cotizacion c = servicio.cotizar("CC-1032", Producto.AUTO, new BigDecimal("80000000"));

        servicio.aceptar(c.id(), "tok_visa");

        Map<String, Object> evento = jdbc.sql("""
                        SELECT type, aggregateid, payload->>'prima' AS prima
                        FROM outbox WHERE aggregateid = :id
                        """)
                .param("id", c.id().toString())
                .query().singleRow();

        assertThat(evento.get("type")).isEqualTo("CotizacionAceptada");
        assertThat(evento.get("prima")).isEqualTo("2800000");
        assertThat(servicio.consultar(c.id()).estado().name()).isEqualTo("ACEPTADA");
    }

    @Test
    void una_aceptacion_repetida_no_publica_un_segundo_evento() {
        Cotizacion c = servicio.cotizar("CC-2044", Producto.HOGAR, new BigDecimal("350000000"));
        servicio.aceptar(c.id(), "tok_visa");

        assertThatThrownBy(() -> servicio.aceptar(c.id(), "tok_visa")).isInstanceOf(TransicionInvalidaException.class);

        Integer eventos = jdbc.sql("SELECT count(*) FROM outbox WHERE aggregateid = :id")
                .param("id", c.id().toString())
                .query(Integer.class).single();
        assertThat(eventos).isEqualTo(1);
    }

    @Test
    void el_outbox_exige_una_transaccion_activa() {
        assertThatThrownBy(() -> outbox.registrar("cotizacion", "x", "Prueba", Map.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
