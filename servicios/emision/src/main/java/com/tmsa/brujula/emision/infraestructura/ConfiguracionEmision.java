package com.tmsa.brujula.emision.infraestructura;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Clock;

/**
 * Configuración transversal del servicio de emisión.
 *
 * <ul>
 *   <li>{@code @EnableResilientMethods}: activa {@code @Retryable} y
 *       {@code @ConcurrencyLimit} nativos de Spring Framework 7 (capítulo 3).</li>
 *   <li>Manejo de errores de Kafka: 3 reintentos y luego el mensaje va al tópico
 *       {@code <tópico>-dlt} en lugar de bloquear la partición.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
class ConfiguracionEmision {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    CommonErrorHandler manejadorDeErroresKafka(KafkaTemplate<Object, Object> kafka) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafka), new FixedBackOff(1000L, 3L));
    }
}
