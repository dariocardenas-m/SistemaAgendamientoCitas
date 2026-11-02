package com.piedrazul.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la cadena HTTP Keycloak con JwtDecoder simulado.
 * No demuestra firma real, login OIDC ni conexion con un servidor Keycloak.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:keycloak_auditoria;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:19800/realms/piedrazul",
        "app.security.jwt.audience=piedrazul-api",
        "app.security.require-https=false"
})
@ActiveProfiles("keycloak")
@AutoConfigureMockMvc
@Transactional
class KeycloakAuthenticationIT {
    private static final String TOKEN = "keycloak-prueba-controlada";

    @Autowired
    MockMvc mvc;

    @MockBean(name = "keycloakJwtDecoder")
    JwtDecoder decoder;

    @BeforeEach
    void tokenAceptadoPorElValidador() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .issuer("http://127.0.0.1:19800/realms/piedrazul")
                .subject("paciente-keycloak-auditoria")
                .audience(List.of("piedrazul-api"))
                .issuedAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("email", "juan.perez@email.com")
                .claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of("PACIENTE")))
                .build();
        when(decoder.decode(TOKEN)).thenReturn(jwt);
    }

    @Test
    void tokenAceptadoDebeConservarLaIdentidadHastaElControlador() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan.perez@email.com"))
                .andExpect(jsonPath("$.rol").value("PACIENTE"));
    }

    @Test
    void pacienteNoDebeAccederALaConsultaDeAgendas() throws Exception {
        mvc.perform(get("/api/citas/consulta")
                        .param("medicoId", "1").param("fecha", "2026-11-02")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void recursoProtegidoSinTokenDebeResponder401() throws Exception {
        mvc.perform(get("/api/citas/mias")).andExpect(status().isUnauthorized());
    }
}
