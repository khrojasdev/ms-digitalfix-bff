package cl.duoc.digitalfix.bff.web;

import cl.duoc.digitalfix.bff.cliente.ClienteUsuarios;
import cl.duoc.digitalfix.bff.cliente.PerfilDeUsuario;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuario;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuarioHolder;
import cl.duoc.digitalfix.bff.error.SolicitudInvalida;
import cl.duoc.digitalfix.bff.web.dto.PerfilRespuesta;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Quien soy y de que empresa soy (HU-04.6).
 *
 * El frontend lo consulta una vez al iniciar sesion para armar la cabecera y
 * decidir que menus mostrar. Los datos de identidad salen del token; la
 * empresa y el rol salen del contexto que ya resolvio el filtro, o sea de
 * APP_USER.
 *
 * El nombre de la empresa se pide aparte porque no cabe en el contexto, que
 * es lo unico que se propaga a los microservicios y ahi el nombre no sirve
 * de nada. No cuesta una llamada extra: ClienteUsuarios ya tiene el perfil
 * en cache de la resolucion de esta misma peticion.
 *
 * Estaba escrito bajo src/test, que es codigo de pruebas y no se empaqueta:
 * el endpoint no existia en la aplicacion en ejecucion.
 */
@RestController
@RequestMapping("/api/me")
public class PerfilController {

    private final ClienteUsuarios usuarios;

    public PerfilController(ClienteUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public PerfilRespuesta perfil(@AuthenticationPrincipal Jwt jwt) {
        ContextoUsuario contexto = ContextoUsuarioHolder.actual();
        if (contexto == null) {
            throw new SolicitudInvalida("No hay contexto de usuario para esta peticion.");
        }

        // Al entrar se sincroniza el perfil con el token: si el rol cambio en
        // el tenant, la base queda al dia y el menu ya sale con el rol nuevo.
        PerfilDeUsuario perfil = jwt == null ? null : usuarios.sincronizar(contexto.oid(), jwt.getTokenValue());
        if (perfil == null && jwt != null) {
            perfil = usuarios.buscarPorOid(contexto.oid(), jwt.getTokenValue());
        }

        return new PerfilRespuesta(
                contexto.oid(),
                correo(jwt),
                jwt == null ? null : jwt.getClaimAsString("name"),
                String.valueOf(contexto.empresaId()),
                perfil == null ? null : perfil.companyName(),
                roles(perfil, jwt, contexto.roles()));
    }

    /**
     * El rol de la base (ya sincronizado) mas los del token, sin repetir. Si no
     * se pudo leer el perfil, los que ya resolvio el contexto.
     */
    static List<String> roles(PerfilDeUsuario perfil, Jwt jwt, List<String> delContexto) {
        if (perfil == null) {
            return delContexto;
        }
        List<String> resultado = new ArrayList<>();
        if (StringUtils.hasText(perfil.role())) {
            resultado.add(perfil.role().trim().toUpperCase());
        }
        List<String> delToken = jwt == null ? null : jwt.getClaimAsStringList("roles");
        if (delToken != null) {
            for (String rol : delToken) {
                String normalizado = rol == null ? "" : rol.trim().toUpperCase();
                if (!normalizado.isEmpty() && !resultado.contains(normalizado)) {
                    resultado.add(normalizado);
                }
            }
        }
        return resultado.isEmpty() ? delContexto : resultado;
    }

    /** En Entra el correo llega en preferred_username, y a veces en email. */
    private String correo(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String preferido = jwt.getClaimAsString("preferred_username");
        return preferido != null ? preferido : jwt.getClaimAsString("email");
    }
}
