package cl.duoc.digitalfix.bff.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.UUID;

/**
 * Propaga el id de correlacion a cada microservicio.
 *
 * El frontend manda X-Correlation-Id en cada peticion, pero el BFF no lo
 * reenviaba: los microservicios publicaban sus eventos sin correlacion y la
 * auditoria, que la exigia, los rechazaba a todos. Ahora cada llamada sale con
 * el id que trajo la peticion; si no trajo uno, se genera, y se reutiliza en
 * todas las llamadas de esa misma peticion.
 */
public class CorrelacionSaliente implements ClientHttpRequestInterceptor {

    public static final String CABECERA = "X-Correlation-Id";
    private static final String ATRIBUTO = CorrelacionSaliente.class.getName();

    @Override
    public ClientHttpResponse intercept(HttpRequest peticion, byte[] cuerpo, ClientHttpRequestExecution ejecucion)
            throws IOException {
        if (!peticion.getHeaders().containsKey(CABECERA)) {
            peticion.getHeaders().set(CABECERA, actual());
        }
        return ejecucion.execute(peticion, cuerpo);
    }

    /** El id de la peticion en curso (cabecera entrante, MDC o uno nuevo). */
    static String actual() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            HttpServletRequest peticion = servlet.getRequest();
            Object guardado = peticion.getAttribute(ATRIBUTO);
            if (guardado instanceof String id) {
                return id;
            }
            String id = limpiar(peticion.getHeader(CABECERA));
            if (id == null) {
                id = limpiar(MDC.get("correlacion"));
            }
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            peticion.setAttribute(ATRIBUTO, id);
            return id;
        }
        String delMdc = limpiar(MDC.get("correlacion"));
        return delMdc != null ? delMdc : UUID.randomUUID().toString();
    }

    /** Solo se acepta un id razonable: letras, numeros y guiones, hasta 100. */
    static String limpiar(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        String recortado = id.trim();
        return recortado.length() <= 100 && recortado.matches("[A-Za-z0-9._:-]+") ? recortado : null;
    }
}
