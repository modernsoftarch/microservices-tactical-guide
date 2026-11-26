package com.tmsa.brujula.cotizacion;

import com.tmsa.brujula.cotizacion.dominio.Producto;
import com.tmsa.brujula.cotizacion.dominio.Tarificador;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de la regla de tarificación (dominio puro, sin Spring). */
class TarificadorTest {

    private final Tarificador tarificador = new Tarificador();

    @Test
    void calcula_la_prima_de_auto_y_redondea_a_pesos() {
        BigDecimal prima = tarificador.calcularPrima(Producto.AUTO, new BigDecimal("80000000"));

        assertThat(prima).isEqualByComparingTo("2800000");
    }

    @Test
    void redondea_a_pesos_enteros() {
        // 1.000.100 × 0,0045 = 4.500,45 → 4.500
        assertThat(tarificador.calcularPrima(Producto.HOGAR, new BigDecimal("1000100")))
                .isEqualByComparingTo("4500");
    }

    @Test
    void rechaza_valores_asegurados_por_debajo_del_minimo() {
        assertThatThrownBy(() -> tarificador.calcularPrima(Producto.VIDA, new BigDecimal("999999")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
