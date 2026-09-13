package cl.duoc.digitalfix.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BffApplicationTests {

    @Test
    void elContextoDeSpringLevanta() {
        // si el contexto no arranca, este test falla solo
    }
}
