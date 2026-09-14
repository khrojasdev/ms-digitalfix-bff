package cl.duoc.digitalfix.bff.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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

    @Test
    void endpointCatalogo_accesoPermitidoTecnico() throws Exception {
        mockMvc.perform(get("/api/catalogo/items")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isNotFound()); // Pasa el filtro de seguridad
    }

    @Test
    void endpointCatalogo_accesoDenegadoUsuarioSinRol() throws Exception {
        mockMvc.perform(get("/api/catalogo/items")
                        .with(jwt())) // JWT válido pero sin authorities
                .andExpect(status().isForbidden());
    }
}