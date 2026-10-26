package com.piedrazul.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Seguridad de producción: JWT firmado y validado contra el emisor de Keycloak. */
@Configuration
@EnableMethodSecurity
@Profile("keycloak")
public class KeycloakSecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;
    private final SecurityAuditAccessDeniedHandler accessDeniedHandler;
    private final boolean requireHttps;

    public KeycloakSecurityConfig(KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
                                  SecurityAuditAccessDeniedHandler accessDeniedHandler,
                                  @Value("${app.security.require-https:true}") boolean requireHttps) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.accessDeniedHandler = accessDeniedHandler;
        this.requireHttps = requireHttps;
    }

    @Bean
    public SecurityFilterChain keycloakSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/especialidades/**", "/api/medicos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/citas/fechas-habiles", "/api/citas/disponibilidad").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").denyAll()
                        .requestMatchers("/h2-console/**").denyAll()
                        .requestMatchers("/api/configuracion/**", "/api/medicos/*/configuracion").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/citas/consulta", "/api/citas/exportar", "/api/citas/historial")
                        .hasAnyRole("AGENDADOR", "ADMINISTRADOR", "MEDICO_TERAPISTA")
                        .requestMatchers("/api/citas/mias", "/api/pacientes/me").hasRole("PACIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/keycloak/perfil-paciente").hasRole("PACIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/keycloak/perfil-basico")
                        .hasAnyRole("ADMINISTRADOR", "AGENDADOR")
                        .requestMatchers(HttpMethod.POST, "/api/citas/**")
                        .hasAnyRole("PACIENTE", "AGENDADOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/citas/**")
                        .hasAnyRole("PACIENTE", "AGENDADOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/atender")
                        .hasAnyRole("AGENDADOR", "ADMINISTRADOR", "MEDICO_TERAPISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/reactivar")
                        .hasAnyRole("AGENDADOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/cancelar")
                        .hasAnyRole("PACIENTE", "AGENDADOR", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
        return http.build();
    }
}
