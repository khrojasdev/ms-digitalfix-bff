package cl.duoc.digitalfix.bff.web;

import cl.duoc.digitalfix.bff.error.ErrorDeMicroservicio;
import cl.duoc.digitalfix.bff.error.ServicioNoDisponible;
import cl.duoc.digitalfix.bff.error.SolicitudInvalida;
import cl.duoc.digitalfix.bff.web.dto.RespuestaError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce cualquier excepcion a un cuerpo de error uniforme.
 *
 * Regla que no se negocia: al cliente nunca le llega un stacktrace ni el
 * nombre de una clase interna. Lo que se registra en el log es otra cosa:
 * ahi si va el detalle, porque el log no lo ve el atacante.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** El microservicio contesto con un error suyo: se devuelve tal cual. */
    @ExceptionHandler(ErrorDeMicroservicio.class)
    public ResponseEntity<String> deMicroservicio(ErrorDeMicroservicio e) {
        return ResponseEntity.status(e.getCodigo())
                .contentType(MediaType.APPLICATION_JSON)
                .body(e.getCuerpo());
    }

    @ExceptionHandler(ServicioNoDisponible.class)
    public ResponseEntity<RespuestaError> noDisponible(ServicioNoDisponible e,
                                                       HttpServletRequest peticion) {
        log.warn("microservicio {} no disponible: {}", e.getServicio(), e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(RespuestaError.de("servicio_no_disponible", e.getMessage(),
                                        peticion.getRequestURI()));
    }

    @ExceptionHandler(SolicitudInvalida.class)
    public ResponseEntity<RespuestaError> invalida(SolicitudInvalida e,
                                                   HttpServletRequest peticion) {
        return ResponseEntity.badRequest()
                .body(RespuestaError.de("solicitud_invalida", e.getMessage(),
                                        peticion.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception e, HttpServletRequest peticion) {
        log.error("error inesperado en {}", peticion.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RespuestaError.de("error_interno",
                                        "Ocurrio un error inesperado.",
                                        peticion.getRequestURI()));
    }
}
