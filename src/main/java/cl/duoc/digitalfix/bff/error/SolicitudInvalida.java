package cl.duoc.digitalfix.bff.error;

/** La peticion no trae lo que el BFF necesita para poder atenderla. */
public class SolicitudInvalida extends RuntimeException {

    public SolicitudInvalida(String mensaje) {
        super(mensaje);
    }
}
