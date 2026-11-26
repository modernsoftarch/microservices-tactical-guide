package com.tmsa.brujula.emision.infraestructura.persistencia;

import com.tmsa.brujula.emision.dominio.RepositorioPolizas;
import com.tmsa.brujula.emision.dominio.SagaEmision;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;

@Repository
class PolizasJdbc implements RepositorioPolizas {

    private final JdbcClient jdbc;

    PolizasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(String numero, SagaEmision saga, Instant emitidaEn) {
        jdbc.sql("""
                        INSERT INTO polizas (numero, cotizacion_id, cliente_id, producto, prima, emitida_en)
                        VALUES (:numero, :cotizacionId, :clienteId, :producto, :prima, :emitidaEn)
                        """)
                .param("numero", numero)
                .param("cotizacionId", saga.cotizacionId())
                .param("clienteId", saga.clienteId())
                .param("producto", saga.producto())
                .param("prima", saga.prima())
                .param("emitidaEn", emitidaEn.atOffset(ZoneOffset.UTC))
                .update();
    }
}
