package com.tmsa.brujula.recaudo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL real en un contenedor para las pruebas de integración.
 *
 * <p>{@code @ServiceConnection} hace que Spring Boot configure el DataSource
 * apuntando al contenedor, sin URLs ni puertos a mano. Necesitas Docker.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedoresDePrueba {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
