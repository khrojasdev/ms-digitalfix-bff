package cl.duoc.digitalfix.bff.web;

import cl.duoc.digitalfix.bff.cliente.ClienteAuditoria;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
public class AuditoriaController {

    private final ClienteAuditoria cliente;

    public AuditoriaController(ClienteAuditoria cliente) {
        this.cliente = cliente;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> proxyAuditoria(HttpServletRequest request, @RequestBody(required = false) String cuerpo) {
        String rutaBase = "/api/audit";
        String rutaDestino = request.getRequestURI().substring(rutaBase.length());
        if (rutaDestino.isEmpty()) {
            rutaDestino = "/";
        }

        MultiValueMap<String, String> parametros = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((key, values) -> parametros.addAll(key, java.util.Arrays.asList(values)));

        return cliente.llamar(HttpMethod.valueOf(request.getMethod()), rutaDestino, parametros, cuerpo);
    }
}