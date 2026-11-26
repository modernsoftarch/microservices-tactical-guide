package com.tmsa.brujula.recaudo.infraestructura;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class ConfiguracionRecaudo {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
