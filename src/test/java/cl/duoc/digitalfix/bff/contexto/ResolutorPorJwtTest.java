package cl.duoc.digitalfix.bff.contexto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import cl.duoc.digitalfix.bff.cliente.ClienteUsuarios;
import cl.duoc.digitalfix.bff.cliente.PerfilDeUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La empresa sale de la base, nunca del token ni del cliente. Estas pruebas
 * cubren los cuatro finales posibles: identidad resuelta, sin token, sin alta
 * en APP_USER y persona desvinculada.
 */
class ResolutorPorJwtTest {

    private final ClienteUsuarios usuarios = mock(ClienteUsuarios.class);
    private final ResolutorPorJwt resolutor = new ResolutorPorJwt(usuarios);
    private final HttpServletRequest peticion = new MockHttpServletRequest();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("con token valido, la empresa es la que dice usuarios")
    void resuelveLaEmpresaDesdeUsuarios() {
        autenticarCon("oid-abc");
        when(usuarios.buscarPorOid(eq("oid-abc"), anyString()))
                .thenReturn(new PerfilDeUsuario("Chris", "chris@duocuc.cl", "ADMIN", 7L, "ElectroRed", true));

        ContextoUsuario contexto = resolutor.resolver(peticion);

        assertThat(contexto).isNotNull();
        assertThat(contexto.oid()).isEqualTo("oid-abc");
        assertThat(contexto.empresaId()).isEqualTo(7L);
        assertThat(contexto.roles()).contains("ADMIN");
    }

    @Test
    @DisplayName("sin token no hay contexto, y ni siquiera se pregunta por el")
    void sinTokenDevuelveNulo() {
        assertThat(resolutor.resolver(peticion)).isNull();
    }

    @Test
    @DisplayName("un token de otra procedencia, sin oid, tampoco resuelve")
    void sinClaimOidDevuelveNulo() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(tokenSinOid(), null));

        assertThat(resolutor.resolver(peticion)).isNull();
    }

    @Test
    @DisplayName("quien no esta dado de alta en su empresa no obtiene contexto")
    void sinPerfilDevuelveNulo() {
        autenticarCon("oid-desconocido");
        when(usuarios.buscarPorOid(anyString(), anyString())).thenReturn(null);

        assertThat(resolutor.resolver(peticion)).isNull();
    }

    @Test
    @DisplayName("un usuario desvinculado existe, pero no entra")
    void desactivadoDevuelveNulo() {
        autenticarCon("oid-fuera");
        when(usuarios.buscarPorOid(anyString(), anyString()))
                .thenReturn(new PerfilDeUsuario("Ex", "ex@duocuc.cl", "SUPERVISOR", 3L, "ElectroRed", false));

        assertThat(resolutor.resolver(peticion)).isNull();
    }

    // ------------------------------------------------------------------ apoyo

    private void autenticarCon(String oid) {
        Jwt token = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("oid", oid)
                .claim("roles", List.of("Admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(token, List.of()));
    }

    private Jwt tokenSinOid() {
        return new Jwt("token-de-prueba",
                Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", "alguien"));
    }
}
