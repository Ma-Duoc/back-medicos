package com.medictime.medicos.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    private static final String CLIENT_ID = "9b818c2c-efc8-487d-9610-5dd0d940f18b";
    private static final String EXPECTED_AUDIENCE = "api://" + CLIENT_ID;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas y de pre-flight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/h2-console/**", "/public/**").permitAll()
                        
                        // 1. REGLAS PARA LECTURA (GET): Administrador y Observadores (incluye la falta de ortografía por compatibilidad)
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority("APPROLE_Administrador", "APPROLE_Obsevador", "APPROLE_Observador")
                        
                        // 2. REGLAS PARA CREACIÓN/EDICIÓN/ELIMINACIÓN (POST, PUT, DELETE): Solo Administrador
                        .requestMatchers(HttpMethod.POST, "/api/**").hasAuthority("APPROLE_Administrador")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasAuthority("APPROLE_Administrador")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAuthority("APPROLE_Administrador")
                        
                        // Cualquier otra petición a la API requiere autenticación
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * Mapea el claim "roles" enviado por Microsoft Entra ID hacia GrantedAuthorities en Spring Security,
     * añadiendo el prefijo "APPROLE_" (ejemplo: "roles": ["Administrador"] -> authority "APPROLE_Administrador").
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("APPROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        // Descarga automáticamente las claves públicas (JWK) de Azure AD
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // Valida que el token esté vigente (exp / nbf)
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

        // Valida que el issuer (iss) corresponda exactamente a nuestro Tenant
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerUri);

        // 🔑 VALIDADOR DE AUDIENCIA FLEXIBLE:
        // Acepta tanto "api://9b818c2c-..." como "9b818c2c-..."
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            List<String> audience = jwt.getAudience();

            if (audience != null && (audience.contains(EXPECTED_AUDIENCE) || audience.contains(CLIENT_ID))) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "The required audience (" + EXPECTED_AUDIENCE + " or " + CLIENT_ID + ") is missing in token: " + audience,
                    null);

            return OAuth2TokenValidatorResult.failure(error);
        };

        // Ejecuta todas las validaciones
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                withTimestamp,
                withIssuer,
                withAudience);

        decoder.setJwtValidator(validator);

        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(List.of(
                "http://localhost:3004",
                "http://localhost:5173",
                "http://localhost:3000"));

        cfg.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"));

        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin"));

        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", cfg);

        return source;
    }
}