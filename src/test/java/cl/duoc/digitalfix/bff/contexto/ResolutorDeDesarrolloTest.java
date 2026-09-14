package cl.duoc.digitalfix.bff.contexto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ResolutorDeDesarrolloTest {

    private PropiedadesContexto propiedades;
    private ResolutorDeDesarrollo resolutor;

    @BeforeEach
    void preparar() {
        propiedades = new PropiedadesContexto();
        propiedades.setModoDesarrollo(true);
        propiedades.setOidPorDefecto("dev-oid");
        propiedades.setEmpresaPorDefecto(1L);
        propiedades.setRolesPorDefecto("Admin");
        resolutor = new ResolutorDeDesarrollo(propiedades);
    }

    @Test
    @DisplayName("fuera del modo desarrollo no resuelve nada, pase lo que pase")
    void sinModoDesarrolloNoResuelve() {
        propiedades.setModoDesarrollo(false);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Dev-Company-Id", "42");

        assertThat(resolutor.resolver(peticion)).isNull();
    }

    @Test
    void tomaLaEmpresaDeLaCabeceraDeDesarrollo() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Dev-Company-Id", "7");
        peticion.addHeader("X-Dev-Roles", "Supervisor, Auditor");

        ContextoUsuario contexto = resolutor.resolver(peticion);

        assertThat(contexto.empresaId()).isEqualTo(7L);
        assertThat(contexto.roles()).containsExactly("Supervisor", "Auditor");
    }

    @Test
    void usaLosValoresDelPerfilCuandoNoVienenCabeceras() {
        ContextoUsuario contexto = resolutor.resolver(new MockHttpServletRequest());

        assertThat(contexto.oid()).isEqualTo("dev-oid");
        assertThat(contexto.empresaId()).isEqualTo(1L);
        assertThat(contexto.roles()).containsExactly("Admin");
    }

    @Test
    @DisplayName("una empresa que no es un numero no revienta: cae al valor por defecto")
    void empresaInvalidaCaeAlPorDefecto() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Dev-Company-Id", "no-soy-un-numero");

        assertThat(resolutor.resolver(peticion).empresaId()).isEqualTo(1L);
    }
}
