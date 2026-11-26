package com.tmsa.brujula.recaudo;

import com.tmsa.brujula.recaudo.aplicacion.ServicioDeCobros;
import com.tmsa.brujula.recaudo.dominio.ClaveReutilizadaException;
import com.tmsa.brujula.recaudo.dominio.CobroEnProcesoException;
import com.tmsa.brujula.recaudo.dominio.EstadoCobro;
import com.tmsa.brujula.recaudo.dominio.PasarelaNoDisponibleException;
import com.tmsa.brujula.recaudo.dominio.ResultadoCobro;
import com.tmsa.brujula.recaudo.dominio.SolicitudCobro;
import com.tmsa.brujula.recaudo.infraestructura.pasarela.PasarelaSimulada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Idempotencia del cobro contra PostgreSQL real (capítulo 3).
 * La pregunta que responde cada prueba: ¿se puede cobrar dos veces?
 */
@SpringBootTest
@Import(ContenedoresDePrueba.class)
class CobroIdempotenteIT {

    @Autowired
    ServicioDeCobros servicio;

    @Autowired
    PasarelaSimulada pasarela;

    @Test
    void un_reintento_con_la_misma_clave_devuelve_el_mismo_resultado_sin_cobrar_otra_vez() {
        String clave = nuevaClave();
        SolicitudCobro solicitud = solicitud("tok_visa");
        int antes = pasarela.cobrosEjecutados();

        ResultadoCobro primero = servicio.cobrar(clave, solicitud);
        ResultadoCobro reintento = servicio.cobrar(clave, solicitud);

        assertThat(primero.estado()).isEqualTo(EstadoCobro.APROBADO);
        assertThat(reintento).isEqualTo(primero);
        assertThat(pasarela.cobrosEjecutados() - antes).isEqualTo(1);
    }

    @Test
    void la_misma_clave_con_otro_cuerpo_se_rechaza() {
        String clave = nuevaClave();
        servicio.cobrar(clave, solicitud("tok_visa"));

        SolicitudCobro otra = new SolicitudCobro("saga-x", "CC-1032", new BigDecimal("999999"), "tok_visa");

        assertThatThrownBy(() -> servicio.cobrar(clave, otra)).isInstanceOf(ClaveReutilizadaException.class);
    }

    @Test
    void un_rechazo_de_la_pasarela_es_un_resultado_de_negocio_y_tambien_se_recuerda() {
        String clave = nuevaClave();

        ResultadoCobro resultado = servicio.cobrar(clave, solicitud("tok_fondos_insuficientes"));

        assertThat(resultado.estado()).isEqualTo(EstadoCobro.RECHAZADO);
        assertThat(resultado.motivo()).isEqualTo("Fondos insuficientes");
        assertThat(servicio.cobrar(clave, solicitud("tok_fondos_insuficientes"))).isEqualTo(resultado);
    }

    @Test
    void si_la_pasarela_no_proceso_la_clave_se_libera_para_reintentar() {
        String clave = nuevaClave();

        assertThatThrownBy(() -> servicio.cobrar(clave, solicitud("tok_caida")))
                .isInstanceOf(PasarelaNoDisponibleException.class);
        // La clave quedó libre: un segundo intento vuelve a llegar a la pasarela.
        assertThatThrownBy(() -> servicio.cobrar(clave, solicitud("tok_caida")))
                .isInstanceOf(PasarelaNoDisponibleException.class);
    }

    @Test
    void diez_peticiones_concurrentes_con_la_misma_clave_cobran_una_sola_vez() throws Exception {
        String clave = nuevaClave();
        SolicitudCobro solicitud = solicitud("tok_visa");
        int antes = pasarela.cobrosEjecutados();

        List<Callable<String>> tareas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tareas.add(() -> {
                try {
                    return servicio.cobrar(clave, solicitud).estado().name();
                } catch (CobroEnProcesoException e) {
                    return "EN_PROCESO";
                }
            });
        }

        List<String> resultados = new ArrayList<>();
        try (ExecutorService hilos = Executors.newFixedThreadPool(10)) {
            for (Future<String> f : hilos.invokeAll(tareas)) {
                resultados.add(f.get());
            }
        }

        // Lo único que importa: la pasarela recibió UN cobro.
        assertThat(pasarela.cobrosEjecutados() - antes).isEqualTo(1);
        // Las demás peticiones ven el resultado guardado (APROBADO) o, si llegaron
        // mientras se cobraba, un 409 (EN_PROCESO) para reintentar después.
        assertThat(resultados).contains("APROBADO").isSubsetOf("APROBADO", "EN_PROCESO");
    }

    private static String nuevaClave() {
        return UUID.randomUUID().toString();
    }

    private static SolicitudCobro solicitud(String token) {
        return new SolicitudCobro("saga-42", "CC-1032", new BigDecimal("2800000"), token);
    }
}
