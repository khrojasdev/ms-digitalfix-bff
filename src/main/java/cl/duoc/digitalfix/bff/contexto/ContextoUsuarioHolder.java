package cl.duoc.digitalfix.bff.contexto;

/**
 * Guarda el contexto de la peticion en curso.
 *
 * Es un ThreadLocal porque el modelo de servlets de Spring MVC atiende cada
 * peticion en su propio hilo. El filtro lo limpia siempre en un finally: si no
 * se limpiara, el hilo se reutiliza y la siguiente peticion heredaria la
 * empresa de la anterior, que es exactamente la fuga que este diseno evita.
 */
public final class ContextoUsuarioHolder {

    private static final ThreadLocal<ContextoUsuario> ACTUAL = new ThreadLocal<>();

    private ContextoUsuarioHolder() {
    }

    public static void fijar(ContextoUsuario contexto) {
        ACTUAL.set(contexto);
    }

    public static ContextoUsuario actual() {
        return ACTUAL.get();
    }

    public static void limpiar() {
        ACTUAL.remove();
    }
}
