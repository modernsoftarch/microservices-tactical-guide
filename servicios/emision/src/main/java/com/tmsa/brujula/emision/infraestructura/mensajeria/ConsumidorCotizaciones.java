package com.tmsa.brujula.emision.infraestructura.mensajeria;

import com.tmsa.brujula.emision.aplicacion.OrquestadorEmision;
import com.tmsa.brujula.emision.dominio.CotizacionAceptada;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Consume los eventos de cotización que Debezium publica desde el outbox.
 *
 * <p>Headers que pone el Outbox Event Router de Debezium:
 * <ul>
 *   <li>{@code id}: id único del evento (fila del outbox). Lo usamos para deduplicar.</li>
 *   <li>{@code eventType}: tipo del evento (configurado en infra/debezium/).</li>
 * </ul>
 * Kafka entrega "al menos una vez": el mismo evento puede llegar dos veces y
 * el orquestador lo descarta gracias a {@code mensajes_procesados}.
 */
@Component
class ConsumidorCotizaciones {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorCotizaciones.class);

    private final OrquestadorEmision orquestador;
    private final JsonMapper json;

    ConsumidorCotizaciones(OrquestadorEmision orquestador, JsonMapper json) {
        this.orquestador = orquestador;
        // Tolerant reader: ignorar campos que todavía no conocemos.
        this.json = json.rebuild().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    @KafkaListener(topics = "${brujula.kafka.topico-cotizaciones}", groupId = "emision",
            autoStartup = "${brujula.kafka.consumir:true}")
    void consumir(ConsumerRecord<String, String> registro) {
        String tipo = header(registro, "eventType").orElse("");
        if (!CotizacionAceptada.TIPO.equals(tipo)) {
            log.debug("Evento {} ignorado", tipo);
            return;
        }
        String idMensaje = header(registro, "id")
                .orElseGet(() -> registro.topic() + "-" + registro.partition() + "-" + registro.offset());

        CotizacionAceptada evento = json.readValue(registro.value(), CotizacionAceptada.class);
        orquestador.alAceptarseCotizacion(idMensaje, evento);
    }

    private static Optional<String> header(ConsumerRecord<?, ?> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            return Optional.empty();
        }
        // Según el converter, el valor puede venir con comillas JSON: las quitamos.
        return Optional.of(new String(h.value(), StandardCharsets.UTF_8).replace("\"", ""));
    }
}
