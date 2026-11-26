package com.tmsa.brujula.emision.infraestructura.persistencia;

import com.tmsa.brujula.emision.dominio.Outbox;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Escribe eventos en la tabla {@code outbox}.
 *
 * <p>{@code Propagation.MANDATORY} es una red de seguridad: si alguien llama a
 * este método fuera de una transacción, falla de inmediato en vez de publicar
 * un evento "huérfano" sin el cambio de negocio que lo originó.
 *
 * <p>Las columnas siguen la convención del Outbox Event Router de Debezium
 * (id, aggregatetype, aggregateid, type, payload). Ver infra/debezium/.
 */
@Component
class OutboxJdbc implements Outbox {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    OutboxJdbc(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String tipoAgregado, String idAgregado, String tipoEvento, Object evento) {
        jdbc.sql("""
                        INSERT INTO outbox (id, aggregatetype, aggregateid, type, payload)
                        VALUES (:id, :tipoAgregado, :idAgregado, :tipoEvento, CAST(:payload AS jsonb))
                        """)
                .param("id", UUID.randomUUID())
                .param("tipoAgregado", tipoAgregado)
                .param("idAgregado", idAgregado)
                .param("tipoEvento", tipoEvento)
                .param("payload", json.writeValueAsString(evento))
                .update();
    }
}
