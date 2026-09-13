package cl.duoc.digitalfix.bff.contexto;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Quita de la peticion todo intento del cliente de decidir de que empresa son
 * los datos.
 *
 * La empresa sale del token y de ningun otro lado. Un cliente puede mandar
 * ?companyId=3 con toda la intencion del mundo: aqui se descarta antes de
 * armar la llamada al microservicio.
 *
 * Se descarta en silencio en vez de responder 400 a proposito. Devolver un
 * error distinto segun lo que mande el cliente le confirmaria que el parametro
 * significa algo; ignorarlo no le dice nada.
 */
public final class SaneadorDeParametros {

    /** Nombres que se descartan, comparados sin distinguir mayusculas. */
    private static final Set<String> PROHIBIDOS = Set.of(
            "companyid", "company_id", "company-id",
            "empresaid", "empresa_id", "empresa-id", "empresa");

    private SaneadorDeParametros() {
    }

    public static boolean esProhibido(String nombre) {
        return nombre != null && PROHIBIDOS.contains(nombre.toLowerCase(Locale.ROOT));
    }

    /**
     * Devuelve los mismos parametros sin los que intentan fijar la empresa.
     */
    public static MultiValueMap<String, String> sanear(Map<String, String[]> parametros) {
        MultiValueMap<String, String> limpios = new LinkedMultiValueMap<>();
        if (parametros == null) {
            return limpios;
        }
        parametros.forEach((nombre, valores) -> {
            if (esProhibido(nombre) || valores == null) {
                return;
            }
            limpios.put(nombre, List.of(valores));
        });
        return limpios;
    }
}
