package cl.duoc.digitalfix.bff.contexto;

import java.io.IOException;
import java.util.List;

import cl.duoc.digitalfix.bff.web.dto.RespuestaError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resuelve el contexto del usuario al principio de cada peticion y lo deja
 * disponible para el resto del BFF.
 *
 * Sin contexto no se atiende nada bajo /api: es preferible un 401 claro a
 * seguir adelante y que un microservicio devuelva datos de una empresa que
 * no corresponde.
 *
 * Actuator queda fuera a proposito, porque lo consultan las sondas del
 * despliegue, que no llevan token.
 *
 * Los resolutores se prueban en orden. Primero el del token, que es el camino
 * real; despues el de desarrollo, que solo responde en el perfil local. El
 * primero que devuelve algo, manda.
 */
@Component
public class ContextoUsuarioFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ContextoUsuarioFilter.class);

    private final List<ResolutorDeContexto> resolutores;
    private final ObjectMapper json;

    public ContextoUsuarioFilter(List<ResolutorDeContexto> resolutores, ObjectMapper json) {
        this.resolutores = resolutores;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        String ruta = peticion.getRequestURI();
        return ruta.startsWith("/actuator") || !ruta.startsWith("/api");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        ContextoUsuario contexto = resolver(peticion);

        if (contexto == null || contexto.empresaId() == null) {
            log.debug("peticion a {} sin contexto resoluble", peticion.getRequestURI());
            responderNoAutorizado(peticion, respuesta);
            return;
        }

        try {
            ContextoUsuarioHolder.fijar(contexto);
            cadena.doFilter(peticion, respuesta);
        } finally {
            // si esto no se limpiara, el siguiente uso de este hilo heredaria
            // la empresa de esta peticion
            ContextoUsuarioHolder.limpiar();
        }
    }

    private ContextoUsuario resolver(HttpServletRequest peticion) {
        for (ResolutorDeContexto resolutor : resolutores) {
            ContextoUsuario contexto = resolutor.resolver(peticion);
            if (contexto != null && contexto.empresaId() != null) {
                return contexto;
            }
        }
        return null;
    }

    private void responderNoAutorizado(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws IOException {
        respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(),
                RespuestaError.de("no_autenticado",
                                  "La peticion no trae una identidad valida.",
                                  peticion.getRequestURI()));
    }
}
