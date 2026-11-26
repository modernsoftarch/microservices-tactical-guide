package com.tmsa.brujula.recaudo.dominio;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Lo que se quiere cobrar.
 *
 * @param referenciaPago referencia del negocio (para emisión: el id de la saga)
 * @param clienteId      cliente al que se le cobra
 * @param monto          valor en COP
 * @param medioDePago    token del medio de pago (nunca el número de tarjeta)
 */
public record SolicitudCobro(String referenciaPago, String clienteId, BigDecimal monto, String medioDePago) {

    public SolicitudCobro {
        Objects.requireNonNull(referenciaPago, "referenciaPago");
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(monto, "monto");
        Objects.requireNonNull(medioDePago, "medioDePago");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
    }

    /**
     * Huella SHA-256 del contenido. Permite detectar a un cliente que reutiliza
     * la misma Idempotency-Key con un cuerpo distinto, lo que es un error suyo
     * y no debe cobrarse ni devolver la respuesta anterior.
     */
    public String huella() {
        String canonico = String.join("|", referenciaPago, clienteId, monto.stripTrailingZeros().toPlainString(), medioDePago);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonico.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
