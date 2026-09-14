package cl.duoc.digitalfix.bff.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para desarrollo local, y solo para desarrollo local.
 *
 * En el despliegue real el CORS lo resuelve el API Gateway (HU-12): el BFF no
 * deberia tener que saber desde que origen lo llaman. Pero en local no hay
 * gateway, y sin esto el navegador bloquea cualquier llamada del frontend en
 * el puerto 4200 al BFF en el 8080.
 *
 * Va atado al perfil local a proposito. Si esto quedara activo en produccion
 * seria una puerta abierta a cualquier origen que se agregue por descuido.
 */
@Configuration
@Profile("local")
public class CorsDesarrolloConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
