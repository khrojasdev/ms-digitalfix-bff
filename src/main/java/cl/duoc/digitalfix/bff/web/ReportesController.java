package cl.duoc.digitalfix.bff.web;

import cl.duoc.digitalfix.bff.cliente.ClienteReportes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportesController {

    private final ClienteReportes cliente;

    public ReportesController(ClienteReportes cliente) {
        this.cliente = cliente;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> proxyReportes(HttpServletRequest request, @RequestBody(required = false) String cuerpo) {
        // La ruta se reenvia completa: el microservicio publica sus endpoints
        // bajo el mismo prefijo que el BFF. Quitarle el prefijo dejaba la
        // llamada en "/" o "/indicators", que alli no existen (404).
        String rutaDestino = request.getRequestURI();

        MultiValueMap<String, String> parametros = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((key, values) -> parametros.addAll(key, java.util.Arrays.asList(values)));

        ResponseEntity<String> respuesta =
                cliente.llamar(HttpMethod.valueOf(request.getMethod()), rutaDestino, parametros, cuerpo);

        // Solo se conserva el tipo de contenido, igual que en el catalogo:
        // reenviar todas las cabeceras del microservicio (Transfer-Encoding,
        // Content-Length) puede corromper la respuesta hacia el navegador.
        HttpHeaders cabeceras = new HttpHeaders();
        MediaType tipo = respuesta.getHeaders().getContentType();
        if (tipo != null) {
            cabeceras.setContentType(tipo);
        }
        return new ResponseEntity<>(respuesta.getBody(), cabeceras, respuesta.getStatusCode());
    }
}