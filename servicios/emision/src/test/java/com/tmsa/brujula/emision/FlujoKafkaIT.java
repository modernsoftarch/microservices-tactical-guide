package com.tmsa.brujula.emision;

import com.tmsa.brujula.emision.dominio.EstadoSaga;
import com.tmsa.brujula.emision.dominio.Recaudo;
import com.tmsa.brujula.emision.dominio.RepositorioSagas;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Prueba de punta a punta del lado consumidor con Kafka real: publica el
 * mensaje tal como lo publicaría Debezium (headers {@code id} y {@code eventType})
 * y verifica que la saga termina, incluso si el mensaje llega dos veces.
 */
@SpringBootTest(properties = {
        "brujula.saga.reintentos-automaticos=false",
        "brujula.kafka.topico-cotizaciones=brujula.cotizacion.prueba"
})
@Import({ContenedoresDePrueba.class, ContenedorKafka.class})
class FlujoKafkaIT {

    private static final String TOPICO = "brujula.cotizacion.prueba";

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    RepositorioSagas sagas;

    @MockitoBean
    Recaudo recaudo;

    @Test
    void un_evento_de_debezium_duplicado_produce_una_sola_poliza() {
        given(recaudo.cobrar(anyString(), anyString(), anyString(), any(), anyString()))
                .willReturn(new Recaudo.ResultadoCobro(true, "AUT-K", null));
        String cotizacionId = UUID.randomUUID().toString();
        String idEvento = UUID.randomUUID().toString();
        String payload = """
                {"cotizacionId":"%s","clienteId":"CC-7788","producto":"AUTO",
                 "valorAsegurado":80000000,"prima":2800000,"medioDePago":"tok_visa",
                 "aceptadaEn":"2026-11-26T15:00:00Z","campoNuevoQueNoConocemos":true}
                """.formatted(cotizacionId);

        enviar(cotizacionId, idEvento, payload);
        enviar(cotizacionId, idEvento, payload); // reentrega: mismo id de evento

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(sagas.buscarPorCotizacion(cotizacionId))
                        .hasValueSatisfying(s -> assertThat(s.estado()).isEqualTo(EstadoSaga.EMITIDA)));
        verify(recaudo, times(1)).cobrar(anyString(), anyString(), anyString(), any(), anyString());
    }

    private void enviar(String clave, String idEvento, String payload) {
        ProducerRecord<String, String> registro = new ProducerRecord<>(TOPICO, clave, payload);
        registro.headers().add("id", idEvento.getBytes(StandardCharsets.UTF_8));
        registro.headers().add("eventType", "CotizacionAceptada".getBytes(StandardCharsets.UTF_8));
        kafka.send(registro).join();
    }
}
