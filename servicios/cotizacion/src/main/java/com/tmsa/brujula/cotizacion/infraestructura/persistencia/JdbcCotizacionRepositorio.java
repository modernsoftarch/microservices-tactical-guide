package com.tmsa.brujula.cotizacion.infraestructura.persistencia;

import com.tmsa.brujula.cotizacion.dominio.Cotizacion;
import com.tmsa.brujula.cotizacion.dominio.CotizacionRepositorio;
import com.tmsa.brujula.cotizacion.dominio.EstadoCotizacion;
import com.tmsa.brujula.cotizacion.dominio.Producto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC. Usamos {@link JdbcClient} en lugar de JPA a propósito:
 * el SQL queda visible y es fácil comparar con lo que muestra el libro.
 */
@Repository
class JdbcCotizacionRepositorio implements CotizacionRepositorio {

    private final JdbcClient jdbc;

    JdbcCotizacionRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Cotizacion c) {
        jdbc.sql("""
                        INSERT INTO cotizaciones (id, cliente_id, producto, valor_asegurado, prima, estado, creada_en)
                        VALUES (:id, :clienteId, :producto, :valorAsegurado, :prima, :estado, :creadaEn)
                        """)
                .param("id", c.id())
                .param("clienteId", c.clienteId())
                .param("producto", c.producto().name())
                .param("valorAsegurado", c.valorAsegurado())
                .param("prima", c.prima())
                .param("estado", c.estado().name())
                .param("creadaEn", c.creadaEn().atOffset(ZoneOffset.UTC))
                .update();
    }

    @Override
    public void actualizarEstado(Cotizacion c) {
        jdbc.sql("UPDATE cotizaciones SET estado = :estado WHERE id = :id")
                .param("estado", c.estado().name())
                .param("id", c.id())
                .update();
    }

    @Override
    public Optional<Cotizacion> buscar(UUID id) {
        return jdbc.sql("""
                        SELECT id, cliente_id, producto, valor_asegurado, prima, estado, creada_en
                        FROM cotizaciones WHERE id = :id
                        """)
                .param("id", id)
                .query(JdbcCotizacionRepositorio::mapear)
                .optional();
    }

    private static Cotizacion mapear(ResultSet rs, int fila) throws SQLException {
        return new Cotizacion(
                rs.getObject("id", UUID.class),
                rs.getString("cliente_id"),
                Producto.valueOf(rs.getString("producto")),
                rs.getBigDecimal("valor_asegurado"),
                rs.getBigDecimal("prima"),
                EstadoCotizacion.valueOf(rs.getString("estado")),
                rs.getObject("creada_en", OffsetDateTime.class).toInstant());
    }
}
