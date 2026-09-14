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
import org.springframework.web.bind.annotation.RestController;

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

        return new PerfilRespuesta(
                contexto.oid(),
                correo(jwt),
                jwt == null ? null : jwt.getClaimAsString("name"),
                String.valueOf(contexto.empresaId()),
                nombreDeEmpresa(contexto, jwt),
                contexto.roles());
    }

    private String nombreDeEmpresa(ContextoUsuario contexto, Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        PerfilDeUsuario perfil = usuarios.buscarPorOid(contexto.oid(), jwt.getTokenValue());
        return perfil == null ? null : perfil.companyName();
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
