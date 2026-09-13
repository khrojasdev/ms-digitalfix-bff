package cl.duoc.digitalfix.bff.error;

/**
 * Un microservicio de dominio no respondio: esta caido, tardo mas de lo
 * permitido, o el cortacircuitos esta abierto porque acaba de fallar varias
 * veces seguidas.
 *
 * Se traduce a 503, nunca a 500: no es un error del BFF, es una dependencia
 * que no esta disponible, y el cliente puede reintentar mas tarde.
 */
public class ServicioNoDisponible extends RuntimeException {

    private final String servicio;

    public ServicioNoDisponible(String servicio, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.servicio = servicio;
    }

    public String getServicio() {
        return servicio;
    }
}
