package cl.duoc.digitalfix.bff.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("api://mi-app-legitima");

    @Test
    void audienciaCorrecta_validaConExito() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("aud", List.of("api://mi-app-legitima"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    void audienciaIncorrecta_rechazaToken() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("aud", List.of("api://otra-app-distinta"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getErrorCode().equals("invalid_token")));
    }
}