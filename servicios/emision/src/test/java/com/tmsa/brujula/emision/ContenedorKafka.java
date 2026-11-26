package com.tmsa.brujula.emision;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;

/** Kafka real (modo KRaft, imagen nativa que arranca en segundos) para pruebas de mensajería. */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedorKafka {

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka-native:4.2.0");
    }
}
