package com.tmsa.brujula.cotizacion.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula la prima anual de una cotización.
 *
 * <p>Regla: {@code prima = valorAsegurado × tasaAnual(producto)}, redondeada a
 * pesos enteros (el COP no maneja centavos en la práctica comercial).
 */
public final class Tarificador {

    /** Valor asegurado mínimo aceptado: evita cotizaciones basura. */
    public static final BigDecimal VALOR_MINIMO = new BigDecimal("1000000");

    public BigDecimal calcularPrima(Producto producto, BigDecimal valorAsegurado) {
        if (valorAsegurado == null || valorAsegurado.compareTo(VALOR_MINIMO) < 0) {
            throw new IllegalArgumentException(
                    "El valor asegurado debe ser al menos " + VALOR_MINIMO.toPlainString());
        }
        return valorAsegurado
                .multiply(producto.tasaAnual())
                .setScale(0, RoundingMode.HALF_UP);
    }
}
