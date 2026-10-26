package com.piedrazul.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@Profile("!keycloak")
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityAuditAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, SecurityAuditAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/especialidades/**", "/api/medicos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/citas/fechas-habiles", "/api/citas/disponibilidad").permitAll()
                        .requestMatchers("/api/admin/**", "/api/configuracion/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/citas/consulta/**").hasAnyRole("AGENDADOR", "ADMINISTRADOR", "MEDICO_TERAPISTA")
                        .requestMatchers(HttpMethod.POST, "/api/citas/**")
                        .hasAnyRole("PACIENTE", "AGENDADOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/citas/**")
                        .hasAnyRole("PACIENTE", "AGENDADOR", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
