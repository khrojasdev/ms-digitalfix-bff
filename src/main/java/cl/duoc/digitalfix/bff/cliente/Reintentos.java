package cl.duoc.digitalfix.bff.cliente;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

/**
 * Reintento minimo para llamadas entre microservicios.
 *
 * Solo se reintenta lo idempotente. Repetir un GET no cambia nada; repetir un
 * POST puede crear dos veces el mismo recurso, asi que no se reintenta aunque
 * parezca que fallo: puede haberse aplicado y haberse perdido la respuesta.
 */
public final class Reintentos {

    private static final Logger log = LoggerFactory.getLogger(Reintentos.class);

    private Reintentos() {
    }

    public static boolean esIdempotente(HttpMethod metodo) {
        return HttpMethod.GET.equals(metodo)
                || HttpMethod.HEAD.equals(metodo)
                || HttpMethod.OPTIONS.equals(metodo)
                || HttpMethod.PUT.equals(metodo)
                || HttpMethod.DELETE.equals(metodo);
    }

    /**
     * Ejecuta la llamada y, si es idempotente y falla por causa tecnica, la
     * repite una sola vez tras una pausa corta.
     */
    public static <T> T conUnReintento(HttpMethod metodo, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RuntimeException primera) {
            if (!esIdempotente(metodo)) {
                throw primera;
            }
            log.debug("reintentando {} tras fallo tecnico: {}", metodo, primera.getMessage());
            pausa();
            return llamada.get();
        }
    }

    private static void pausa() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
