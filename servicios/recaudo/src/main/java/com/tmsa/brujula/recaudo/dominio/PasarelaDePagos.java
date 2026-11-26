package com.tmsa.brujula.recaudo.dominio;

/**
 * Puerto hacia el proveedor de pagos. El dominio habla de "aprobado/rechazado"
 * con motivos de negocio; los códigos del proveedor se traducen en el adaptador
 * (capa anticorrupción, capítulo 3).
 */
public interface PasarelaDePagos {

    /**
     * @throws PasarelaNoDisponibleException si el proveedor confirma que no procesó
     *         la operación (por ejemplo, 503). En ese caso es seguro reintentar.
     */
    ResultadoCobro cobrar(SolicitudCobro solicitud);
}
