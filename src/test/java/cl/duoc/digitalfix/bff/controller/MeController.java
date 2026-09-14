package cl.duoc.digitalfix.bff.controller;

import cl.duoc.digitalfix.bff.controller.dto.UserProfileDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public UserProfileDTO getProfile(@AuthenticationPrincipal Jwt jwt) {
        // Extraemos los claims estándar de Entra ID / Microsoft
        String oid = jwt.getClaimAsString("oid");
        String name = jwt.getClaimAsString("name");

        // En Entra ID, el correo suele venir en "preferred_username" o "email"
        String email = jwt.hasClaim("preferred_username")
                ? jwt.getClaimAsString("preferred_username")
                : jwt.getClaimAsString("email");

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            roles = List.of(); // Evita nulos si el usuario no tiene roles
        }

        // TODO: A futuro, hacer llamada HTTP al ms-usuarios usando el "oid"
        // para recuperar la empresa real. Por ahora, datos simulados.
        String mockCompanyId = "EMP-001";
        String mockCompanyName = "Ultraport";

        return new UserProfileDTO(
                oid,
                email,
                name,
                mockCompanyId,
                mockCompanyName,
                roles
        );
    }
}