package cl.duoc.digitalfix.bff.contexto;

/**
 * Nombres de las cabeceras con que el BFF le pasa el contexto a los
 * microservicios de dominio.
 *
 * Son un contrato interno: los microservicios confian en ellas porque solo el
 * BFF puede hablarles. Si alguna vez quedan expuestos directamente, esto deja
 * de ser seguro y habria que firmarlas o validar el token tambien alli.
 */
public final class CabecerasDeContexto {

    public static final String OID = "X-User-Oid";
    public static final String EMPRESA = "X-Company-Id";
    public static final String ROLES = "X-Roles";

    private CabecerasDeContexto() {
    }
}
