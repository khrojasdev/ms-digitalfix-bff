package cl.duoc.digitalfix.bff.cliente;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lo que ms-digitalfix-usuarios sabe de quien esta llamando.
 *
 * Es el contrato de GET /api/users/{oid}. Se ignoran los campos que no se
 * usan aqui para que el BFF no se rompa cada vez que usuarios agregue uno.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfilDeUsuario(String name, String email, String role,
                              Long companyId, Boolean active) {

    public boolean estaActivo() {
        return Boolean.TRUE.equals(active);
    }
}
