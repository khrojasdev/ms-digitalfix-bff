package cl.duoc.digitalfix.bff.web;

import cl.duoc.digitalfix.bff.cliente.ClienteCatalogo;
import cl.duoc.digitalfix.bff.contexto.SaneadorDeParametros;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Puerta del frontend hacia el catalogo.
 *
 * Es un paso a traves deliberado y no un controlador con un metodo por
 * endpoint. El catalogo ya define su contrato y sus validaciones; repetir aqui
 * cada DTO solo agregaria un sitio mas donde quedarse atrasado. Lo que el BFF
 * si aporta en el camino es lo que el catalogo no puede saber por si mismo:
 * quien llama, de que empresa es, y que el companyId del cliente no cuenta.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogoController {

    private final ClienteCatalogo catalogo;

    public CatalogoController(ClienteCatalogo catalogo) {
        this.catalogo = catalogo;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest peticion,
                                        @RequestBody(required = false) String cuerpo) {
        HttpMethod metodo = HttpMethod.valueOf(peticion.getMethod());
        String ruta = peticion.getRequestURI();
        MultiValueMap<String, String> parametros =
                SaneadorDeParametros.sanear(peticion.getParameterMap());

        ResponseEntity<String> respuesta = catalogo.llamar(metodo, ruta, parametros, cuerpo);

        // se conserva el tipo de contenido del catalogo; el resto de sus
        // cabeceras no se reenvia, para no filtrar detalles de su despliegue
        HttpHeaders cabeceras = new HttpHeaders();
        MediaType tipo = respuesta.getHeaders().getContentType();
        if (tipo != null) {
            cabeceras.setContentType(tipo);
        }

        return new ResponseEntity<>(respuesta.getBody(), cabeceras, respuesta.getStatusCode());
    }
}
