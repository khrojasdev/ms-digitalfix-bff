package cl.duoc.digitalfix.bff.contexto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * De donde sale el contexto del usuario.
 *
 * Hoy hay una sola implementacion, la de desarrollo, porque el tenant de
 * identidad todavia no existe (T-02) y el resource server aun no esta
 * configurado (HU-05).
 *
 * Cuando HU-05 llegue, se agrega una implementacion que lee el JWT validado
 * y consulta al microservicio de usuarios por el oid. Nada mas del BFF cambia:
 * esa es la razon de que esto sea una interfaz y no codigo suelto dentro del
 * filtro.
 */
public interface ResolutorDeContexto {

    /**
     * @return el contexto de quien llama, o null si no se puede determinar.
     */
    ContextoUsuario resolver(HttpServletRequest peticion);
}
