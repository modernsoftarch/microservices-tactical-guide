package com.tmsa.brujula.cotizacion.dominio;

/**
 * Puerto para publicar eventos de integración con el patrón Outbox.
 *
 * <p>La implementación escribe el evento en la tabla {@code outbox} dentro de la
 * MISMA transacción que el cambio de negocio. Debezium lee esa tabla desde el
 * log de PostgreSQL y la publica en Kafka. Así evitamos el "doble write":
 * o se guardan ambos (cambio + evento) o ninguno. Ver docs/decisiones/ADR-001.
 */
public interface Outbox {

    /**
     * @param tipoAgregado  tipo de agregado; Debezium lo usa para elegir el tópico
     * @param idAgregado    id del agregado; se usa como key del mensaje (orden por agregado)
     * @param tipoEvento    nombre del evento, viaja en el header {@code eventType}
     * @param evento        objeto a serializar como JSON
     */
    void registrar(String tipoAgregado, String idAgregado, String tipoEvento, Object evento);
}
