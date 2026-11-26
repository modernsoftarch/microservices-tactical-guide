package com.tmsa.brujula.cotizacion.dominio;

import java.math.BigDecimal;

/**
 * Productos que cotiza Seguros Brújula y su tasa anual sobre el valor asegurado.
 *
 * <p>Las tasas son didácticas. En una aseguradora real vienen de la nota
 * técnica del actuario y cambian por perfil de riesgo; aquí basta una tasa fija
 * para mostrar dónde vive la regla.
 */
public enum Producto {
    AUTO(new BigDecimal("0.035")),
    HOGAR(new BigDecimal("0.0045")),
    VIDA(new BigDecimal("0.012"));

    private final BigDecimal tasaAnual;

    Producto(BigDecimal tasaAnual) {
        this.tasaAnual = tasaAnual;
    }

    public BigDecimal tasaAnual() {
        return tasaAnual;
    }
}
