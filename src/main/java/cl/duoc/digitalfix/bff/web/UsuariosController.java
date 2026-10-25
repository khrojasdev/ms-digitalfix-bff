package cl.duoc.digitalfix.bff.web;

import cl.duoc.digitalfix.bff.cliente.ClienteUsuarios;
import cl.duoc.digitalfix.bff.cliente.PerfilDeUsuario;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuario;
import cl.duoc.digitalfix.bff.contexto.ContextoUsuarioHolder;
import cl.duoc.digitalfix.bff.error.SolicitudInvalida;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nombre y correo de una persona a partir de su oid.
 *
 * Lo usa la vista de auditoria para mostrar quien hizo cada accion y para
 * filtrar por usuario sin tener que pegar el identificador de Entra ID.
 *
 * Solo responde por personas de la misma empresa de quien consulta: un oid de
 * otra empresa se responde igual que uno inexistente (404), para no revelar
 * que existe.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final ClienteUsuarios usuarios;

    public UsuariosController(ClienteUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/{oid}")
    public ResponseEntity<UsuarioResumen> resumen(@PathVariable String oid,
                                                  @AuthenticationPrincipal Jwt jwt) {
        ContextoUsuario contexto = ContextoUsuarioHolder.actual();
        if (contexto == null) {
            throw new SolicitudInvalida("No hay contexto de usuario para esta peticion.");
        }

        PerfilDeUsuario perfil = usuarios.consultar(oid, jwt == null ? null : jwt.getTokenValue());
        if (perfil == null || perfil.companyId() == null
                || !perfil.companyId().equals(contexto.empresaId())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new UsuarioResumen(oid, perfil.name(), perfil.email(), perfil.role()));
    }

    /** Lo minimo para mostrar a la persona: nada de empresa ni estado. */
    public record UsuarioResumen(String oid, String nombre, String email, String rol) {
    }
}
