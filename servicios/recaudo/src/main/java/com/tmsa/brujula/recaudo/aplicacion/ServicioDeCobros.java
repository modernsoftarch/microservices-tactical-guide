package com.tmsa.brujula.recaudo.aplicacion;

import com.tmsa.brujula.recaudo.dominio.ClaveReutilizadaException;
import com.tmsa.brujula.recaudo.dominio.CobroEnProcesoException;
import com.tmsa.brujula.recaudo.dominio.CobrosRepositorio;
import com.tmsa.brujula.recaudo.dominio.EstadoCobro;
import com.tmsa.brujula.recaudo.dominio.PasarelaDePagos;
import com.tmsa.brujula.recaudo.dominio.PasarelaNoDisponibleException;
import com.tmsa.brujula.recaudo.dominio.RegistroCobro;
import com.tmsa.brujula.recaudo.dominio.ResultadoCobro;
import com.tmsa.brujula.recaudo.dominio.SolicitudCobro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Cobro idempotente (capítulo 3).
 *
 * <p>Algoritmo:
 * <ol>
 *   <li>Reservar la Idempotency-Key con {@code INSERT ... ON CONFLICT DO NOTHING}.
 *       La base de datos decide quién gana si llegan dos peticiones a la vez.</li>
 *   <li>Si la clave ya existía: devolver el resultado guardado, o 409 si sigue
 *       en proceso, o 422 si el cuerpo es distinto.</li>
 *   <li>Si la clave es nueva: llamar a la pasarela FUERA de la transacción
 *       (no queremos una conexión de BD abierta mientras esperamos a un tercero)
 *       y guardar el resultado.</li>
 * </ol>
 *
 * <p>Ojo con los timeouts: si la pasarela no responde, NO sabemos si cobró.
 * En ese caso la clave queda EN_PROCESO y se resuelve con conciliación, nunca
 * liberándola a ciegas. Solo liberamos cuando el proveedor confirma que no
 * procesó nada ({@link PasarelaNoDisponibleException}).
 */
@Service
public class ServicioDeCobros {

    private final CobrosRepositorio repositorio;
    private final PasarelaDePagos pasarela;
    private final TransactionTemplate tx;
    private final Clock reloj;

    public ServicioDeCobros(CobrosRepositorio repositorio, PasarelaDePagos pasarela,
                            TransactionTemplate tx, Clock reloj) {
        this.repositorio = repositorio;
        this.pasarela = pasarela;
        this.tx = tx;
        this.reloj = reloj;
    }

    public ResultadoCobro cobrar(String clave, SolicitudCobro solicitud) {
        String huella = solicitud.huella();

        Boolean reservada = tx.execute(estado -> repositorio.reservar(clave, huella, solicitud, reloj.instant()));
        if (!Boolean.TRUE.equals(reservada)) {
            return resolverRepetida(clave, huella);
        }

        ResultadoCobro resultado;
        try {
            resultado = pasarela.cobrar(solicitud);
        } catch (PasarelaNoDisponibleException e) {
            tx.executeWithoutResult(estado -> repositorio.liberar(clave));
            throw e;
        }

        tx.executeWithoutResult(estado -> repositorio.completar(clave, resultado));
        return resultado;
    }

    private ResultadoCobro resolverRepetida(String clave, String huella) {
        RegistroCobro previo = repositorio.buscar(clave)
                .orElseThrow(() -> new CobroEnProcesoException(clave)); // se liberó justo ahora: reintenta
        if (!previo.huella().equals(huella)) {
            throw new ClaveReutilizadaException(clave);
        }
        if (previo.estado() == EstadoCobro.EN_PROCESO) {
            throw new CobroEnProcesoException(clave);
        }
        return previo.comoResultado();
    }
}
