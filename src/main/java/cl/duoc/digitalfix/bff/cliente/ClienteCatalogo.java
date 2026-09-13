package cl.duoc.digitalfix.bff.cliente;

import java.time.Duration;

import cl.duoc.digitalfix.bff.contexto.CabecerasDeContexto;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuario;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuarioHolder;
import cl.duoc.digitalfix.bff.error.ServicioNoDisponible;
import cl.duoc.digitalfix.bff.error.SolicitudInvalida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Habla con ms-digitalfix-catalog.
 *
 * Dos reglas que sostienen el aislamiento entre empresas:
 *
 *  1. El contexto que se propaga es SIEMPRE el que resolvio el BFF. Las
 *     cabeceras de contexto que venga trayendo el cliente no se reenvian: se
 *     escriben de cero aqui.
 *  2. Los codigos de error del catalogo se devuelven tal cual. Si el catalogo
 *     responde 404 porque el recurso es de otra empresa, el frontend tiene que
 *     ver ese 404. Convertirlo en 500 esconderia justo lo que hay que ver.
 */
@Component
public class ClienteCatalogo {

    private static final Logger log = LoggerFactory.getLogger(ClienteCatalogo.class);

    private static final String NOMBRE = "catalogo";
    private static final String AVISO =
            "El catalogo no esta disponible en este momento. Intenta de nuevo en unos segundos.";

    private final RestClient cliente;
    private final Cortacircuitos cortacircuitos =
            new Cortacircuitos(3, Duration.ofSeconds(15));

    public ClienteCatalogo(RestClient clienteCatalogo) {
        this.cliente = clienteCatalogo;
    }

    public ResponseEntity<String> llamar(HttpMethod metodo, String ruta,
                                         MultiValueMap<String, String> parametros,
                                         String cuerpo) {
        ContextoUsuario contexto = ContextoUsuarioHolder.actual();
        if (contexto == null) {
            throw new SolicitudInvalida("No hay contexto de usuario para esta peticion.");
        }

        if (cortacircuitos.estaAbierto()) {
            // no se intenta siquiera: el catalogo acaba de fallar varias veces
            // seguidas y cada intento cuesta un timeout completo
            throw new ServicioNoDisponible(NOMBRE, AVISO, null);
        }

        log.debug("catalogo <- {} {} empresa={}", metodo, ruta, contexto.empresaId());

        try {
            ResponseEntity<String> respuesta = Reintentos.conUnReintento(
                    metodo, () -> ejecutar(metodo, ruta, parametros, cuerpo, contexto));
            cortacircuitos.registrarExito();
            return respuesta;
        } catch (ResourceAccessException e) {
            // no contesto: esta caido o tardo mas de lo permitido
            cortacircuitos.registrarFallo();
            log.warn("el catalogo no respondio a {} {}: {}", metodo, ruta, e.getMessage());
            throw new ServicioNoDisponible(NOMBRE, AVISO, e);
        }
    }

    private ResponseEntity<String> ejecutar(HttpMethod metodo, String ruta,
                                            MultiValueMap<String, String> parametros,
                                            String cuerpo, ContextoUsuario contexto) {
        RestClient.RequestBodySpec peticion = cliente.method(metodo)
                .uri(constructor -> constructor.path(ruta).queryParams(parametros).build())
                .headers(cabeceras -> ponerContexto(cabeceras, contexto));

        if (StringUtils.hasText(cuerpo)) {
            peticion = peticion.contentType(MediaType.APPLICATION_JSON);
            peticion = peticion.body(cuerpo);
        }

        return peticion.retrieve()
                // el estado de error no lanza: se devuelve tal cual vino
                .onStatus(HttpStatusCode::isError, (solicitud, respuesta) -> { })
                .toEntity(String.class);
    }

    private void ponerContexto(HttpHeaders cabeceras, ContextoUsuario contexto) {
        cabeceras.set(CabecerasDeContexto.OID, contexto.oid());
        cabeceras.set(CabecerasDeContexto.EMPRESA, String.valueOf(contexto.empresaId()));
        cabeceras.set(CabecerasDeContexto.ROLES, contexto.rolesComoTexto());
    }
}
