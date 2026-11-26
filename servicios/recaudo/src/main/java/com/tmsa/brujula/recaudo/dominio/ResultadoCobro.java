package com.tmsa.brujula.recaudo.dominio;

/**
 * Resultado de negocio de un cobro. Un rechazo NO es un error técnico: es una
 * respuesta válida que la saga de emisión debe manejar.
 *
 * @param estado      APROBADO o RECHAZADO
 * @param referencia  referencia de la transacción en la pasarela (si se aprobó)
 * @param motivo      motivo del rechazo en lenguaje del negocio (si se rechazó)
 */
public record ResultadoCobro(EstadoCobro estado, String referencia, String motivo) {

    public static ResultadoCobro aprobado(String referencia) {
        return new ResultadoCobro(EstadoCobro.APROBADO, referencia, null);
    }

    public static ResultadoCobro rechazado(String motivo) {
        return new ResultadoCobro(EstadoCobro.RECHAZADO, null, motivo);
    }
}
