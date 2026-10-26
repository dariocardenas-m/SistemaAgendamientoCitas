package com.piedrazul.usuarios;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keycloak/perfil-paciente")
@Profile("keycloak")
public class PerfilPacienteKeycloakController {

    private final PerfilPacienteKeycloakService service;

    public PerfilPacienteKeycloakController(PerfilPacienteKeycloakService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public UsuarioDto crear(@Valid @RequestBody PerfilPacienteKeycloakRequest request) {
        return service.crearPerfil(request);
    }
}
