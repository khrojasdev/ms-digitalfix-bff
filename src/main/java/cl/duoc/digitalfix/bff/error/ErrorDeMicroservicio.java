package cl.duoc.digitalfix.bff.error;

/**
 * El microservicio respondio, pero con un error de negocio (400, 404, 409...).
 *
 * El BFF no lo reinterpreta: devuelve el mismo codigo y el mismo cuerpo. Si el
 * catalogo dice 404 porque el recurso es de otra empresa, el frontend tiene que
 * ver ese mismo 404 y no un 500 del intermediario.
 */
public class ErrorDeMicroservicio extends RuntimeException {

    private final int codigo;
    private final String cuerpo;

    public ErrorDeMicroservicio(int codigo, String cuerpo) {
        super("el microservicio respondio " + codigo);
        this.codigo = codigo;
        this.cuerpo = cuerpo;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getCuerpo() {
        return cuerpo;
    }
}
