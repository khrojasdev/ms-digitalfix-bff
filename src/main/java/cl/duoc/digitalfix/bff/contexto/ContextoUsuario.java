package cl.duoc.digitalfix.bff.contexto;

import java.util.List;

/**
 * Quien esta llamando: su identificador, su empresa y sus roles.
 *
 * Estos tres datos salen SIEMPRE del token, nunca del cuerpo ni del query
 * string. Es la pieza que sostiene el aislamiento entre empresas: si el
 * companyId pudiera venir del cliente, cualquiera veria los datos de otra
 * empresa cambiando un numero.
 */
public record ContextoUsuario(String oid, Long empresaId, List<String> roles) {

    public ContextoUsuario {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public String rolesComoTexto() {
        return String.join(",", roles);
    }
}
