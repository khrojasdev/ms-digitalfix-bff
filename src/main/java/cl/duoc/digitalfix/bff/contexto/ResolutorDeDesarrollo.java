package cl.duoc.digitalfix.bff.contexto;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resuelve el contexto sin token, para poder trabajar mientras el tenant de
 * identidad no existe.
 *
 * Acepta X-Dev-Oid, X-Dev-Company-Id y X-Dev-Roles, y si no vienen usa los
 * valores del perfil. SOLO actua cuando digitalfix.contexto.modo-desarrollo
 * es true, que unicamente esta activado en el perfil local: en cualquier otro
 * entorno devuelve null y la peticion termina en 401.
 *
 * Esto no es una puerta trasera disimulada, es andamio con fecha de retiro:
 * se borra entero cuando HU-05 configure el resource server.
 */
@Component
public class ResolutorDeDesarrollo implements ResolutorDeContexto {

    private final PropiedadesContexto propiedades;

    public ResolutorDeDesarrollo(PropiedadesContexto propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public ContextoUsuario resolver(HttpServletRequest peticion) {
        if (!propiedades.isModoDesarrollo()) {
            return null;
        }

        String oid = valor(peticion.getHeader("X-Dev-Oid"), propiedades.getOidPorDefecto());
        Long empresa = empresaDe(peticion.getHeader("X-Dev-Company-Id"));
        List<String> roles = rolesDe(
                valor(peticion.getHeader("X-Dev-Roles"), propiedades.getRolesPorDefecto()));

        return new ContextoUsuario(oid, empresa, roles);
    }

    private Long empresaDe(String cabecera) {
        if (!StringUtils.hasText(cabecera)) {
            return propiedades.getEmpresaPorDefecto();
        }
        try {
            return Long.valueOf(cabecera.trim());
        } catch (NumberFormatException e) {
            return propiedades.getEmpresaPorDefecto();
        }
    }

    private List<String> rolesDe(String texto) {
        if (!StringUtils.hasText(texto)) {
            return List.of();
        }
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String valor(String cabecera, String porDefecto) {
        return StringUtils.hasText(cabecera) ? cabecera.trim() : porDefecto;
    }
}
