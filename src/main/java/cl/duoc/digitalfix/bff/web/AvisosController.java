package cl.duoc.digitalfix.bff.web;

import cl.duoc.digitalfix.bff.cliente.ClienteNotify;
import cl.duoc.digitalfix.bff.contexto.SaneadorDeParametros;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Puerta del frontend hacia los avisos de notify.
 *
 * Paso a traves hacia ms-digitalfix-notify: expone los avisos de cambio de
 * estado y de stock que notify genera al consumir la mensajeria. Solo lectura.
 */
@RestController
@RequestMapping("/api/avisos")
public class AvisosController {

    private final ClienteNotify notify;

    public AvisosController(ClienteNotify notify) {
        this.notify = notify;
    }

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    public ResponseEntity<String> proxy(HttpServletRequest peticion,
                                        @RequestBody(required = false) String cuerpo) {
        HttpMethod metodo = HttpMethod.valueOf(peticion.getMethod());
        String ruta = peticion.getRequestURI();
        MultiValueMap<String, String> parametros =
                SaneadorDeParametros.sanear(peticion.getParameterMap());

        ResponseEntity<String> respuesta = notify.llamar(metodo, ruta, parametros, cuerpo);

        HttpHeaders cabeceras = new HttpHeaders();
        MediaType tipo = respuesta.getHeaders().getContentType();
        if (tipo != null) {
            cabeceras.setContentType(tipo);
        }

        return new ResponseEntity<>(respuesta.getBody(), cabeceras, respuesta.getStatusCode());
    }
}
