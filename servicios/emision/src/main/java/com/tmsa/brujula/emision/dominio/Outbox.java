package com.tmsa.brujula.emision.dominio;

/** Publicación de eventos con el patrón Outbox (misma transacción que el cambio). */
public interface Outbox {

    void registrar(String tipoAgregado, String idAgregado, String tipoEvento, Object evento);
}
