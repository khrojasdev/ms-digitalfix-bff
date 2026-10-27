package cl.duoc.digitalfix.bff.config;

import cl.duoc.digitalfix.bff.error.JwtAccessDeniedHandler;
import cl.duoc.digitalfix.bff.error.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
    private String audience;

    // Origenes permitidos para CORS, configurables por entorno. Por defecto el
    // frontend local; en el despliegue se agrega el origen publico con la
    // variable CORS_ALLOWED_ORIGINS (uno o varios, separados por coma).
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}")
    private String corsAllowedOrigins;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Busca automáticamente el bean corsConfigurationSource
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 1. Endpoints públicos o de salud
                        .requestMatchers("/actuator/health").permitAll()

                        // 2. Módulo de Auditoría: ADMIN y AUDITOR, igual que valida
                        // ms-digitalfix-audit y que muestra el menu del frontend.
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")

                        // 2.1 Nombres de usuario para la vista de auditoria: mismos roles.
                        .requestMatchers("/api/usuarios/**").hasAnyRole("ADMIN", "AUDITOR")

                        // 3. Módulos de Gestión
                        .requestMatchers("/api/dashboard/**", "/api/reports/**").hasAnyRole("ADMIN", "SUPERVISOR")

                        // 3.1 Avisos (notify): lectura para quienes operan y supervisan
                        .requestMatchers("/api/avisos/**").hasAnyRole("ADMIN", "SUPERVISOR", "AUDITOR")

                        // 3.2 Ordenes de trabajo: crear es de quien opera o solicita; cambiar
                        // el estado, de quien gestiona. AUDITOR solo lee. Sin estas reglas, la
                        // general de abajo dejaba a AUDITOR y CLIENTE cambiar estados llamando
                        // a la API directamente (el frontend solo esconde el boton).
                        //
                        // Tecnicos de terreno (todas las operaciones): los gestiona quien
                        // asigna. Va antes de las reglas por metodo para que el POST de
                        // CLIENTE no alcance a dar de alta tecnicos.
                        .requestMatchers("/api/workorders/tecnicos/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        //
                        // TECNICO: ve sus ordenes y marca el avance en terreno. El
                        // microservicio revisa ademas que la orden sea suya y que no
                        // la cancele.
                        .requestMatchers(HttpMethod.GET, "/api/workorders/mias").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.PUT, "/api/workorders/*/status").hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO")
                        // Detalle de una orden: el TECNICO tambien (workorders solo le
                        // muestra las suyas). El listado completo sigue cerrado para el.
                        .requestMatchers(HttpMethod.GET, "/api/workorders/*").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE", "AUDITOR", "TECNICO")
                        .requestMatchers(HttpMethod.POST, "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR")

                        // 4. Módulos Operativos.
                        // El TECNICO solo lee los servicios del catálogo (para ver qué
                        // trabajo es cada orden); no entra a repuestos ni al resto.
                        .requestMatchers(HttpMethod.GET, "/api/catalog/services", "/api/catalog/services/**")
                                .hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE", "AUDITOR", "TECNICO")
                        .requestMatchers("/api/catalog/**", "/api/repuestos/**", "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE", "AUDITOR")

                        // 5. Fallback
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);
        return jwtDecoder;
    }

    // CORS: origenes desde CORS_ALLOWED_ORIGINS (ver campo corsAllowedOrigins).
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(corsAllowedOrigins.split(","))
                      .map(String::trim)
                      .filter(origen -> !origen.isEmpty())
                      .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}