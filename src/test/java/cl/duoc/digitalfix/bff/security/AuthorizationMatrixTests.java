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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    // "Permitido" = pasa el filtro de seguridad. El microservicio no esta
    // levantado en las pruebas, asi que la respuesta es 503 y no 401/403.
    @Test
    void endpointAuditoria_accesoPermitidoAdmin() throws Exception {
        mockMvc.perform(get("/api/audit/events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void endpointAuditoria_accesoPermitidoAuditor() throws Exception {
        mockMvc.perform(get("/api/audit/events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void endpointAuditoria_accesoDenegadoSupervisor() throws Exception {
        mockMvc.perform(get("/api/audit/events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarEstadoOrden_permitidoSupervisor() throws Exception {
        mockMvc.perform(put("/api/workorders/1/estado").param("nuevoEstado", "ASIGNADA")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void cambiarEstadoOrden_denegadoAuditor() throws Exception {
        mockMvc.perform(put("/api/workorders/1/estado").param("nuevoEstado", "CERRADA")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarEstadoOrden_denegadoCliente() throws Exception {
        mockMvc.perform(put("/api/workorders/1/estado").param("nuevoEstado", "CERRADA")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarEstadoPorStatus_permitidoSupervisor() throws Exception {
        mockMvc.perform(put("/api/workorders/1/status").contentType("application/json")
                        .content("{\"status\":\"EN_DESPLAZAMIENTO\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void asignarTecnico_denegadoCliente() throws Exception {
        mockMvc.perform(put("/api/workorders/1/asignacion").contentType("application/json")
                        .content("{\"tecnicoId\":1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnicos_listarPermitidoSupervisor() throws Exception {
        mockMvc.perform(get("/api/workorders/tecnicos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void tecnicos_crearPermitidoAdmin() throws Exception {
        mockMvc.perform(post("/api/workorders/tecnicos").contentType("application/json")
                        .content("{\"nombre\":\"Pedro\",\"email\":\"pedro@digitalfix.cl\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isServiceUnavailable());
    }

    // Sin la regla propia, el POST general de ordenes dejaba al CLIENTE crear tecnicos.
    @Test
    void tecnicos_crearDenegadoCliente() throws Exception {
        mockMvc.perform(post("/api/workorders/tecnicos").contentType("application/json")
                        .content("{\"nombre\":\"Pedro\",\"email\":\"pedro@digitalfix.cl\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnicos_listarDenegadoClienteYAuditor() throws Exception {
        mockMvc.perform(get("/api/workorders/tecnicos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/workorders/tecnicos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnicos_desactivarDenegadoAuditor() throws Exception {
        mockMvc.perform(delete("/api/workorders/tecnicos/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnico_veSusOrdenes() throws Exception {
        mockMvc.perform(get("/api/workorders/mias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void misOrdenes_soloTecnico() throws Exception {
        mockMvc.perform(get("/api/workorders/mias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnico_marcaElAvanceDeSuOrden() throws Exception {
        mockMvc.perform(put("/api/workorders/1/status").contentType("application/json")
                        .content("{\"status\":\"EN_DESPLAZAMIENTO\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void tecnico_noAsignaNiCreaNiVeTodasLasOrdenes() throws Exception {
        mockMvc.perform(put("/api/workorders/1/asignacion").contentType("application/json")
                        .content("{\"tecnicoId\":1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/workorders").contentType("application/json")
                        .content("{\"servicioId\":1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/workorders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/workorders/tecnicos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnico_veElDetalleDeUnaOrden() throws Exception {
        mockMvc.perform(get("/api/workorders/5")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void cliente_veElDetalleDeUnaOrden() throws Exception {
        mockMvc.perform(get("/api/workorders/5")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void tecnico_leeServiciosPeroNoRepuestos() throws Exception {
        mockMvc.perform(get("/api/catalog/services")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/repuestos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/catalog/services").contentType("application/json").content("{}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearOrden_permitidoCliente() throws Exception {
        mockMvc.perform(post("/api/workorders").contentType("application/json")
                        .content("{\"servicioId\":1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void crearOrden_denegadoAuditor() throws Exception {
        mockMvc.perform(post("/api/workorders").contentType("application/json")
                        .content("{\"servicioId\":1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
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