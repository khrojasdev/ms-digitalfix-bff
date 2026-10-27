package cl.duoc.digitalfix.bff.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/** El X-Correlation-Id de la peticion llega a los microservicios. */
class CorrelacionSalienteTest {

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private MockHttpServletRequest peticionCon(String id) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (id != null) {
            peticion.addHeader(CorrelacionSaliente.CABECERA, id);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));
        return peticion;
    }

    @Test
    void reenviaElIdQueTrajoLaPeticion() {
        peticionCon("abc-123");
        assertThat(CorrelacionSaliente.actual()).isEqualTo("abc-123");
    }

    @Test
    void sinIdGeneraUnoYLoReutilizaEnLaMismaPeticion() {
        peticionCon(null);
        String primero = CorrelacionSaliente.actual();
        assertThat(primero).isNotBlank();
        assertThat(CorrelacionSaliente.actual()).isEqualTo(primero);
    }

    @Test
    void descartaUnIdConCaracteresRaros() {
        peticionCon("<script>");
        assertThat(CorrelacionSaliente.actual()).isNotEqualTo("<script>").matches("[0-9a-f-]{36}");
    }

    @Test
    void fueraDeUnaPeticionTambienDevuelveUno() {
        assertThat(CorrelacionSaliente.actual()).isNotBlank();
    }
}
