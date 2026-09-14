package cl.duoc.digitalfix.bff.web.dto;

import java.util.List;

/**
 * Lo que el frontend necesita saber de la sesion actual.
 *
 * Es lo que pinta la cabecera del frontend: nombre de la persona, empresa y
 * roles. companyName viene de ms-digitalfix-usuarios; si ese servicio no
 * contesta, llega null y la cabecera se queda sin el dato en vez de romperse.
 */
public record PerfilRespuesta(
        String oid,
        String email,
        String name,
        String companyId,
        String companyName,
        List<String> roles
) {}
