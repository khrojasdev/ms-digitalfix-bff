package cl.duoc.digitalfix.bff.contexto;

import java.util.ArrayList;
import java.util.List;

import cl.duoc.digitalfix.bff.cliente.ClienteUsuarios;
import cl.duoc.digitalfix.bff.cliente.PerfilDeUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resuelve el contexto a partir del token ya validado por el resource server.
 *
 * Este es el camino de verdad, el que hace real la multi-tenencia por
 * aplicacion: el token trae el oid de la persona dentro del unico tenant de
 * identidad, y la empresa sale de APP_USER, no del token. Por eso hay que
 * preguntarle a ms-digitalfix-usuarios.
 *
 * El rol tambien se toma de APP_USER cuando esta. Los roles del token sirven
 * para autorizar rutas en el BFF, pero el rol que un microservicio necesita
 * ver es el que esa persona tiene EN ESA EMPRESA, y eso vive en la base.
 *
 * Corre antes que ResolutorDeDesarrollo. Si no hay token, o el token no trae
 * oid, o esa persona no esta dada de alta, devuelve null y deja que decida el
 * siguiente resolutor: en local eso permite seguir trabajando con cabeceras
 * de desarrollo, y en cualquier otro entorno significa 401.
 */
@Component
@Order(10)
public class ResolutorPorJwt implements ResolutorDeContexto {

    private static final Logger log = LoggerFactory.getLogger(ResolutorPorJwt.class);

    private final ClienteUsuarios usuarios;

    public ResolutorPorJwt(ClienteUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public ContextoUsuario resolver(HttpServletRequest peticion) {
        Jwt token = tokenActual();
        if (token == null) {
            return null;
        }

        String oid = token.getClaimAsString("oid");
        if (!StringUtils.hasText(oid)) {
            // en un token de Entra el oid siempre viene; si falta, es un token
            // de otra procedencia y no hay identidad que resolver
            log.debug("token sin claim oid");
            return null;
        }

        PerfilDeUsuario perfil = usuarios.buscarPorOid(oid, token.getTokenValue());
        if (perfil == null || perfil.companyId() == null) {
            return null;
        }

        if (!perfil.estaActivo()) {
            // desvinculado: existe, pero ya no puede entrar
            log.info("el oid {} esta desactivado en su empresa", oid);
            return null;
        }

        return new ContextoUsuario(oid, perfil.companyId(), roles(perfil, token));
    }

    /**
     * El rol de la base manda. Los del token se suman por si el BFF crece y
     * alguien define permisos que no dependen de la empresa.
     */
    private List<String> roles(PerfilDeUsuario perfil, Jwt token) {
        List<String> resultado = new ArrayList<>();

        if (StringUtils.hasText(perfil.role())) {
            resultado.add(perfil.role().trim().toUpperCase());
        }

        List<String> delToken = token.getClaimAsStringList("roles");
        if (delToken != null) {
            for (String rol : delToken) {
                String normalizado = rol == null ? "" : rol.trim().toUpperCase();
                if (!normalizado.isEmpty() && !resultado.contains(normalizado)) {
                    resultado.add(normalizado);
                }
            }
        }

        return resultado;
    }

    private Jwt tokenActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken conJwt) {
            return conJwt.getToken();
        }
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
