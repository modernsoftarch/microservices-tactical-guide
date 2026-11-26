package com.tmsa.brujula.cotizacion.infraestructura;

import com.tmsa.brujula.cotizacion.dominio.Tarificador;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Registra como beans las piezas del dominio que no dependen de Spring. */
@Configuration(proxyBeanMethods = false)
class ConfiguracionCotizacion {

    @Bean
    Tarificador tarificador() {
        return new Tarificador();
    }

    /** Reloj inyectable: en pruebas se puede reemplazar por uno fijo. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
