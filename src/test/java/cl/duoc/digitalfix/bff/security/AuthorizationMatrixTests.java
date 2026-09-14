package cl.duoc.digitalfix.bff.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationMatrixTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void endpointActuator_accesoPermitidoSinToken() throws Exception {
        // La seguridad lo deja pasar, y actuator puede o no estar ahí (200 o 404), pero nunca 401/403
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointAuditoria_accesoPermitidoSoloAdmin() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound()); // Pasa el filtro de seguridad
    }

    @Test
    void endpointAuditoria_accesoDenegadoTecnico() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isForbidden()); // Bloqueado por falta de privilegios
    }

    // La ruta real del catálogo es /api/catalog/**, en inglés como el resto del
    // contrato. Los casos de abajo apuntaban a /api/catalogo/**, que no la
    // toca: pasaban por el fallback y no probaban la regla que dicen probar.
    @Test
    void endpointCatalogo_accesoPermitidoSupervisor() throws Exception {
        // 503 y no 404: pasa la seguridad, llega al proxy y el catálogo no está
        // levantado en las pruebas. Lo que importa aquí es que no sea 401 ni 403.
        mockMvc.perform(get("/api/catalog/services")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void endpointCatalogo_accesoPermitidoAuditor() throws Exception {
        mockMvc.perform(get("/api/catalog/services")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void endpointCatalogo_accesoDenegadoUsuarioSinRol() throws Exception {
        mockMvc.perform(get("/api/catalog/services")
                        .with(jwt())) // JWT válido pero sin authorities
                .andExpect(status().isForbidden());
    }
}