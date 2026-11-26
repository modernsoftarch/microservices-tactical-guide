package com.tmsa.brujula.emision.infraestructura.recaudo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Crea el cliente de recaudo con timeouts explícitos.
 *
 * <p>Regla del capítulo 3: toda llamada remota lleva timeout de conexión y de
 * lectura, y el de lectura es menor que el tiempo que el que nos llama está
 * dispuesto a esperar.
 */
@Configuration(proxyBeanMethods = false)
class ConfiguracionClienteRecaudo {

    @Bean
    RecaudoCliente recaudoCliente(RestClient.Builder builder,
                                  @Value("${brujula.recaudo.url}") String url,
                                  @Value("${brujula.recaudo.timeout-conexion:PT1S}") Duration timeoutConexion,
                                  @Value("${brujula.recaudo.timeout-lectura:PT3S}") Duration timeoutLectura) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(timeoutConexion).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(timeoutLectura);

        RestClient restClient = builder.baseUrl(url).requestFactory(fabrica).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(RecaudoCliente.class);
    }
}
