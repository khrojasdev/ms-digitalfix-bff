package cl.duoc.digitalfix.bff.cliente;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Habla con ms-digitalfix-usuarios para saber a que empresa pertenece quien
 * llama.
 *
 * Esta es la pieza que convierte una identidad de Entra en una empresa del
 * sistema. El token dice quien eres (oid); la tabla APP_USER dice de que
 * empresa eres y con que rol. Lo segundo no puede salir del token, porque el
 * tenant es uno solo para las veinte empresas.
 *
 * El resultado se guarda un minuto. Sin eso, cada peticion al catalogo se
 * convierte en dos saltos de red en vez de uno, y la empresa de una persona
 * no cambia de un segundo a otro.
 */
@Component
public class ClienteUsuarios {

    private static final Logger log = LoggerFactory.getLogger(ClienteUsuarios.class);

    private static final Duration VIGENCIA = Duration.ofSeconds(60);

    private final RestClient cliente;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public ClienteUsuarios(@Qualifier("restClientUsuarios") RestClient restClientUsuarios) {
        this.cliente = restClientUsuarios;
    }

    /**
     * @param oid         identificador de la persona en el tenant
     * @param tokenCrudo  el mismo token que llego al BFF; usuarios tambien lo exige
     * @return el perfil, o null si no existe, esta desactivado o usuarios no responde
     */
    public PerfilDeUsuario buscarPorOid(String oid, String tokenCrudo) {
        Entrada guardada = cache.get(oid);
        if (guardada != null && guardada.vigente()) {
            return guardada.perfil();
        }

        PerfilDeUsuario perfil = pedir(oid, tokenCrudo);

        // tambien se guarda el null: si alguien que no esta dado de alta insiste,
        // no tiene sentido preguntar por el en cada peticion
        cache.put(oid, new Entrada(perfil, Instant.now().plus(VIGENCIA)));
        return perfil;
    }

    private PerfilDeUsuario pedir(String oid, String tokenCrudo) {
        try {
            return cliente.get()
                    .uri("/api/users/{oid}", oid)
                    .headers(cabeceras -> autorizar(cabeceras, tokenCrudo))
                    .retrieve()
                    .onStatus(estado -> estado.value() == 404, (peticion, respuesta) -> {
                        // todavia no esta dado de alta en APP_USER: no es un fallo
                        // del sistema, es una persona sin empresa asignada
                        throw new SinPerfil();
                    })
                    .body(PerfilDeUsuario.class);
        } catch (SinPerfil e) {
            log.info("el oid {} no tiene perfil en usuarios", oid);
            return null;
        } catch (ResourceAccessException e) {
            log.warn("usuarios no respondio al resolver el oid {}: {}", oid, e.getMessage());
            return null;
        } catch (RuntimeException e) {
            log.warn("no se pudo resolver el oid {} contra usuarios: {}", oid, e.getMessage());
            return null;
        }
    }

    private void autorizar(HttpHeaders cabeceras, String tokenCrudo) {
        if (tokenCrudo != null && !tokenCrudo.isBlank()) {
            cabeceras.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCrudo);
        }
    }

    /** Se usa solo para limpiar entre pruebas. */
    public void olvidarTodo() {
        cache.clear();
    }

    private record Entrada(PerfilDeUsuario perfil, Instant vence) {
        boolean vigente() {
            return Instant.now().isBefore(vence);
        }
    }

    /** Corta el flujo sin cargar una traza: un 404 aqui es un caso esperado. */
    private static final class SinPerfil extends RuntimeException {
        private SinPerfil() {
            super(null, null, false, false);
        }
    }
}
