package com.piedrazul.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeycloakJwtAuthenticationConverterTest {
    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void aceptaUnRolDeAplicacionEIgnoraLosRolesTecnicosDeKeycloak() {
        var authentication = converter.convert(token(List.of("offline_access", "PACIENTE", "uma_authorization")));
        assertEquals(List.of("ROLE_PACIENTE"), authentication.getAuthorities().stream()
                .map(Object::toString).toList());
    }

    @Test
    void rechazaUnTokenConDosRolesDeAplicacion() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> converter.convert(token(List.of("ADMINISTRADOR", "PACIENTE"))));
    }

    @Test
    void rechazaUnaIdentidadSinRolDeAplicacion() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> converter.convert(token(List.of("offline_access", "uma_authorization"))));
    }

    private Jwt token(List<String> roles) {
        return Jwt.withTokenValue("token-de-prueba").header("alg", "RS256")
                .subject("identidad-de-prueba")
                .claim("email", "audit@example.test")
                .claim("realm_access", Map.of("roles", roles)).build();
    }
}
