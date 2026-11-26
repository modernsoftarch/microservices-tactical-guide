package com.tmsa.brujula.emision.dominio;

import java.time.Instant;

/** Puerto de persistencia de pólizas emitidas. */
public interface RepositorioPolizas {

    void guardar(String numero, SagaEmision saga, Instant emitidaEn);
}
