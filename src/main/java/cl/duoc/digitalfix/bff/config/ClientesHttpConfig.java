package cl.duoc.digitalfix.bff.config;

import java.time.Duration;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP hacia los microservicios de dominio.
 *
 * Cada cliente lleva tiempo de espera explícito. Un cliente sin timeout es una
 * bomba de relojería: si el microservicio se cuelga, el hilo del BFF se queda
 * esperando para siempre y termina cayéndose el BFF entero por algo que no es
 * suyo.
 *
 * No se registra aquí ningún manejador de errores: el propio cliente de cada
 * microservicio decide qué hacer con cada código.
 */
@Configuration
public class ClientesHttpConfig {

    /**
     * OJO con el nombre: no puede ser "clienteCatalogo". Un método @Bean registra
     * el bean con el nombre del método, y ya existe una clase ClienteCatalogo
     * anotada con @Component, que registra ese mismo nombre. Spring aborta el
     * arranque antes que sobreescribir uno con el otro, y hace bien.
     */
    @Bean
    public RestClient restClientCatalogo(PropiedadesMicroservicios propiedades) {
        return crearCliente(propiedades.getCatalogo());
    }

    /**
     * Cliente hacia ms-digitalfix-usuarios. Se usa para resolver la empresa de
     * quien llama, así que su timeout tiene que ser corto: esta llamada está en
     * el camino de TODAS las peticiones, y si se demora, todo se demora.
     */
    @Bean
    public RestClient restClientUsuarios(PropiedadesMicroservicios propiedades) {
        return crearCliente(propiedades.getUsuarios());
    }

    @Bean
    public RestClient restClientReportes(PropiedadesMicroservicios propiedades) {
        return crearCliente(propiedades.getReportes());
    }

    @Bean
    public RestClient restClientAuditoria(PropiedadesMicroservicios propiedades) {
        return crearCliente(propiedades.getAuditoria());
    }

    @Bean
    public RestClient restClientWorkorders(PropiedadesMicroservicios propiedades) {
        return crearCliente(propiedades.getWorkorders());
    }

    @Bean
    public RestClient restClientNotify(PropiedadesMicroservicios propiedades) {
        return crearCliente(propiedades.getNotify());
    }

    // --- Métodos Auxiliares ---

    private RestClient crearCliente(PropiedadesMicroservicios.Destino destino) {
        return RestClient.builder()
                .baseUrl(destino.getUrl())
                .requestFactory(fabrica(destino))
                // Cada llamada lleva el X-Correlation-Id de la peticion original.
                .requestInterceptor(new CorrelacionSaliente())
                .build();
    }

    private ClientHttpRequestFactory fabrica(PropiedadesMicroservicios.Destino destino) {
        ClientHttpRequestFactorySettings ajustes = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(destino.getTimeoutConexionMs()))
                .withReadTimeout(Duration.ofMillis(destino.getTimeoutLecturaMs()));
        return ClientHttpRequestFactories.get(ajustes);
    }
}