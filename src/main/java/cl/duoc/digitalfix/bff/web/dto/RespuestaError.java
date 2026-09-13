package cl.duoc.digitalfix.bff.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo uniforme de error del BFF.
 *
 * Tiene la misma forma que el del microservicio de catalogo a proposito: el
 * frontend lee siempre los mismos campos, venga el error de donde venga.
 * Nunca lleva stacktrace ni detalles internos.
 */
public record RespuestaError(String error, String message, Instant timestamp,
                             String path, List<String> detalles) {

    public static RespuestaError de(String error, String mensaje, String path) {
        return new RespuestaError(error, mensaje, Instant.now(), path, null);
    }

    public static RespuestaError de(String error, String mensaje, String path,
                                    List<String> detalles) {
        return new RespuestaError(error, mensaje, Instant.now(), path, detalles);
    }
}
