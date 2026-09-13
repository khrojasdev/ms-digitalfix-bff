package cl.duoc.digitalfix.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * BFF de DigitalFix.
 *
 * Es la unica puerta que ve el frontend. Sus tres trabajos, en este orden:
 * validar el token, resolver la empresa y los roles de quien llama, y
 * orquestar a los microservicios de dominio propagandoles ese contexto.
 *
 * No tiene base de datos: todo lo que devuelve viene de otro servicio.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
