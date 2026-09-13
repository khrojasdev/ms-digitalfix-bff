package cl.duoc.digitalfix.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Direcciones y tiempos de espera de los microservicios de dominio.
 *
 * Los valores viven en application.yml y se sobreescriben por variables de
 * entorno. Ninguna direccion ni credencial se escribe en el codigo.
 */
@ConfigurationProperties(prefix = "digitalfix.microservicios")
public class PropiedadesMicroservicios {

    private Destino catalogo = new Destino();

    public Destino getCatalogo() {
        return catalogo;
    }

    public void setCatalogo(Destino catalogo) {
        this.catalogo = catalogo;
    }

    public static class Destino {

        private String url = "http://localhost:8082";

        /** Cuanto se espera a que el otro extremo acepte la conexion. */
        private int timeoutConexionMs = 2000;

        /** Cuanto se espera la respuesta una vez conectado. */
        private int timeoutLecturaMs = 5000;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getTimeoutConexionMs() {
            return timeoutConexionMs;
        }

        public void setTimeoutConexionMs(int timeoutConexionMs) {
            this.timeoutConexionMs = timeoutConexionMs;
        }

        public int getTimeoutLecturaMs() {
            return timeoutLecturaMs;
        }

        public void setTimeoutLecturaMs(int timeoutLecturaMs) {
            this.timeoutLecturaMs = timeoutLecturaMs;
        }
    }
}
