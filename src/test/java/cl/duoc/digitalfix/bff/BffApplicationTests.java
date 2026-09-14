package cl.duoc.digitalfix.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest
@ActiveProfiles("test")
class BffApplicationTests {

    @MockBean
    private JwtDecoder jwtDecoder; // Intercepta el arranque y evita la llamada de red

    @Test
    void elContextoDeSpringLevanta() {
        // si el contexto no arranca, este test falla solo
    }
}
