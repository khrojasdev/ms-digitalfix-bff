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
 * Cada cliente lleva tiempo de espera explicito. Un cliente sin timeout es una
 * bomba de relojeria: si el microservicio se cuelga, el hilo del BFF se queda
 * esperando para siempre y termina cayendose el BFF entero por algo que no es
 * suyo.
 *
 * No se registra aqui ningun manejador de errores: el propio cliente de cada
 * microservicio decide que hacer con cada codigo.
 */
@Configuration
public class ClientesHttpConfig {

    /**
     * OJO con el nombre: no puede ser "clienteCatalogo". Un metodo @Bean registra
     * el bean con el nombre del metodo, y ya existe una clase ClienteCatalogo
     * anotada con @Component, que registra ese mismo nombre. Spring aborta el
     * arranque antes que sobreescribir uno con el otro, y hace bien.
     */
    @Bean
    public RestClient restClientCatalogo(PropiedadesMicroservicios propiedades) {
        PropiedadesMicroservicios.Destino destino = propiedades.getCatalogo();
        return RestClient.builder()
                .baseUrl(destino.getUrl())
                .requestFactory(fabrica(destino))
                .build();
    }

    /**
     * Cliente hacia ms-digitalfix-usuarios. Se usa para resolver la empresa de
     * quien llama, asi que su timeout tiene que ser corto: esta llamada esta en
     * el camino de TODAS las peticiones, y si se demora, todo se demora.
     */
    @Bean
    public RestClient restClientUsuarios(PropiedadesMicroservicios propiedades) {
        PropiedadesMicroservicios.Destino destino = propiedades.getUsuarios();
        return RestClient.builder()
                .baseUrl(destino.getUrl())
                .requestFactory(fabrica(destino))
                .build();
    }

    private ClientHttpRequestFactory fabrica(PropiedadesMicroservicios.Destino destino) {
        ClientHttpRequestFactorySettings ajustes = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(destino.getTimeoutConexionMs()))
                .withReadTimeout(Duration.ofMillis(destino.getTimeoutLecturaMs()));
        return ClientHttpRequestFactories.get(ajustes);
    }
}
