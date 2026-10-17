package cl.duoc.digitalfix.bff.cliente;

import java.time.Duration;

import cl.duoc.digitalfix.bff.contexto.CabecerasDeContexto;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuario;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuarioHolder;
import cl.duoc.digitalfix.bff.error.ServicioNoDisponible;
import cl.duoc.digitalfix.bff.error.SolicitudInvalida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

@Component
public class ClienteWorkorders {

    private static final Logger log = LoggerFactory.getLogger(ClienteWorkorders.class);

    private static final String NOMBRE = "workorders";
    private static final String AVISO = "El servicio de ordenes de trabajo no esta disponible en este momento. Intenta de nuevo en unos segundos.";

    private final RestClient cliente;
    private final Cortacircuitos cortacircuitos = new Cortacircuitos(3, Duration.ofSeconds(15));

    public ClienteWorkorders(@Qualifier("restClientWorkorders") RestClient restClientWorkorders) {
        this.cliente = restClientWorkorders;
    }

    public ResponseEntity<String> llamar(HttpMethod metodo, String ruta, MultiValueMap<String, String> parametros, String cuerpo) {
        ContextoUsuario contexto = ContextoUsuarioHolder.actual();
        if (contexto == null) {
            throw new SolicitudInvalida("No hay contexto de usuario para esta peticion.");
        }

        if (cortacircuitos.estaAbierto()) {
            throw new ServicioNoDisponible(NOMBRE, AVISO, null);
        }

        log.debug("workorders <- {} {} empresa={}", metodo, ruta, contexto.empresaId());

        try {
            ResponseEntity<String> respuesta = Reintentos.conUnReintento(
                    metodo, () -> ejecutar(metodo, ruta, parametros, cuerpo, contexto));
            cortacircuitos.registrarExito();
            return respuesta;
        } catch (ResourceAccessException e) {
            cortacircuitos.registrarFallo();
            log.warn("el servicio de ordenes no respondio a {} {}: {}", metodo, ruta, e.getMessage());
            throw new ServicioNoDisponible(NOMBRE, AVISO, e);
        }
    }

    private ResponseEntity<String> ejecutar(HttpMethod metodo, String ruta, MultiValueMap<String, String> parametros, String cuerpo, ContextoUsuario contexto) {
        RestClient.RequestBodySpec peticion = cliente.method(metodo)
                .uri(constructor -> constructor.path(ruta).queryParams(parametros).build())
                .headers(cabeceras -> ponerContexto(cabeceras, contexto));

        if (StringUtils.hasText(cuerpo)) {
            peticion = peticion.contentType(MediaType.APPLICATION_JSON);
            peticion = peticion.body(cuerpo);
        }

        return peticion.retrieve()
                .onStatus(HttpStatusCode::isError, (solicitud, respuesta) -> { })
                .toEntity(String.class);
    }

    private void ponerContexto(HttpHeaders cabeceras, ContextoUsuario contexto) {
        cabeceras.set(CabecerasDeContexto.OID, contexto.oid());
        cabeceras.set(CabecerasDeContexto.EMPRESA, String.valueOf(contexto.empresaId()));
        cabeceras.set(CabecerasDeContexto.ROLES, contexto.rolesComoTexto());
    }
}