package com.tmsa.brujula.emision.infraestructura.persistencia;

import com.tmsa.brujula.emision.dominio.EstadoSaga;
import com.tmsa.brujula.emision.dominio.RepositorioSagas;
import com.tmsa.brujula.emision.dominio.SagaEmision;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SagasJdbc implements RepositorioSagas {

    private static final String COLUMNAS = """
            id, cotizacion_id, cliente_id, producto, prima, medio_de_pago, estado, intentos, numero_poliza, motivo
            """;

    private final JdbcClient jdbc;

    SagasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Consumidor idempotente: la PRIMARY KEY descarta el mensaje repetido. */
    @Override
    public boolean registrarMensaje(String idMensaje) {
        return jdbc.sql("INSERT INTO mensajes_procesados (id_mensaje) VALUES (:id) ON CONFLICT DO NOTHING")
                .param("id", idMensaje)
                .update() == 1;
    }

    @Override
    public SagaEmision crearSiNoExiste(SagaEmision s) {
        jdbc.sql("""
                        INSERT INTO sagas_emision (id, cotizacion_id, cliente_id, producto, prima, medio_de_pago, estado)
                        VALUES (:id, :cotizacionId, :clienteId, :producto, :prima, :medioDePago, :estado)
                        ON CONFLICT (cotizacion_id) DO NOTHING
                        """)
                .param("id", s.id())
                .param("cotizacionId", s.cotizacionId())
                .param("clienteId", s.clienteId())
                .param("producto", s.producto())
                .param("prima", s.prima())
                .param("medioDePago", s.medioDePago())
                .param("estado", s.estado().name())
                .update();
        return buscarPorCotizacion(s.cotizacionId()).orElseThrow();
    }

    @Override
    public Optional<SagaEmision> buscarPorCotizacion(String cotizacionId) {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM sagas_emision WHERE cotizacion_id = :c")
                .param("c", cotizacionId)
                .query(SagasJdbc::mapear)
                .optional();
    }

    @Override
    public boolean transicionar(UUID id, EstadoSaga desde, EstadoSaga hacia, String numeroPoliza, String motivo) {
        return jdbc.sql("""
                        UPDATE sagas_emision
                        SET estado = :hacia, numero_poliza = :numero, motivo = :motivo, actualizada_en = now()
                        WHERE id = :id AND estado = :desde
                        """)
                .param("hacia", hacia.name())
                .param("numero", numeroPoliza)
                .param("motivo", motivo)
                .param("id", id)
                .param("desde", desde.name())
                .update() == 1;
    }

    @Override
    public void registrarIntentoFallido(UUID id, String error) {
        jdbc.sql("""
                        UPDATE sagas_emision
                        SET intentos = intentos + 1, ultimo_error = left(:error, 500), actualizada_en = now()
                        WHERE id = :id
                        """)
                .param("error", error)
                .param("id", id)
                .update();
    }

    @Override
    public List<SagaEmision> pendientes(Instant antesDe, int limite) {
        return jdbc.sql("SELECT " + COLUMNAS + """
                         FROM sagas_emision
                        WHERE estado = 'COBRO_PENDIENTE' AND actualizada_en < :antesDe
                        ORDER BY actualizada_en
                        LIMIT :limite
                        """)
                .param("antesDe", antesDe.atOffset(ZoneOffset.UTC))
                .param("limite", limite)
                .query(SagasJdbc::mapear)
                .list();
    }

    private static SagaEmision mapear(ResultSet rs, int fila) throws SQLException {
        return new SagaEmision(
                rs.getObject("id", UUID.class),
                rs.getString("cotizacion_id"),
                rs.getString("cliente_id"),
                rs.getString("producto"),
                rs.getBigDecimal("prima"),
                rs.getString("medio_de_pago"),
                EstadoSaga.valueOf(rs.getString("estado")),
                rs.getInt("intentos"),
                rs.getString("numero_poliza"),
                rs.getString("motivo"));
    }
}
