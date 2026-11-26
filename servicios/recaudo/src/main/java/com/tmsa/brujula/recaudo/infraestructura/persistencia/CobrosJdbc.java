package com.tmsa.brujula.recaudo.infraestructura.persistencia;

import com.tmsa.brujula.recaudo.dominio.CobrosRepositorio;
import com.tmsa.brujula.recaudo.dominio.EstadoCobro;
import com.tmsa.brujula.recaudo.dominio.RegistroCobro;
import com.tmsa.brujula.recaudo.dominio.ResultadoCobro;
import com.tmsa.brujula.recaudo.dominio.SolicitudCobro;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
class CobrosJdbc implements CobrosRepositorio {

    private final JdbcClient jdbc;

    CobrosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * El corazón de la idempotencia: la PRIMARY KEY sobre la clave hace que solo
     * una inserción gane, aunque lleguen dos peticiones en el mismo milisegundo.
     */
    @Override
    public boolean reservar(String clave, String huella, SolicitudCobro s, Instant ahora) {
        int filas = jdbc.sql("""
                        INSERT INTO cobros (clave_idempotencia, huella, referencia_pago, cliente_id, monto, estado, creado_en)
                        VALUES (:clave, :huella, :referenciaPago, :clienteId, :monto, 'EN_PROCESO', :ahora)
                        ON CONFLICT (clave_idempotencia) DO NOTHING
                        """)
                .param("clave", clave)
                .param("huella", huella)
                .param("referenciaPago", s.referenciaPago())
                .param("clienteId", s.clienteId())
                .param("monto", s.monto())
                .param("ahora", ahora.atOffset(ZoneOffset.UTC))
                .update();
        return filas == 1;
    }

    @Override
    public Optional<RegistroCobro> buscar(String clave) {
        return jdbc.sql("""
                        SELECT clave_idempotencia, huella, estado, referencia_pasarela, motivo
                        FROM cobros WHERE clave_idempotencia = :clave
                        """)
                .param("clave", clave)
                .query((rs, fila) -> new RegistroCobro(
                        rs.getString("clave_idempotencia"),
                        rs.getString("huella"),
                        EstadoCobro.valueOf(rs.getString("estado")),
                        rs.getString("referencia_pasarela"),
                        rs.getString("motivo")))
                .optional();
    }

    @Override
    public void completar(String clave, ResultadoCobro r) {
        jdbc.sql("""
                        UPDATE cobros SET estado = :estado, referencia_pasarela = :referencia, motivo = :motivo
                        WHERE clave_idempotencia = :clave AND estado = 'EN_PROCESO'
                        """)
                .param("estado", r.estado().name())
                .param("referencia", r.referencia())
                .param("motivo", r.motivo())
                .param("clave", clave)
                .update();
    }

    @Override
    public void liberar(String clave) {
        jdbc.sql("DELETE FROM cobros WHERE clave_idempotencia = :clave AND estado = 'EN_PROCESO'")
                .param("clave", clave)
                .update();
    }
}
