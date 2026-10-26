package com.piedrazul.usuarios;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keycloak/perfil-basico")
@Profile("keycloak")
public class PerfilBasicoKeycloakController {

    private final PerfilBasicoKeycloakService service;

    public PerfilBasicoKeycloakController(PerfilBasicoKeycloakService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AGENDADOR')")
    public UsuarioDto crear(@Valid @RequestBody PerfilBasicoKeycloakRequest request) {
        return service.crearPerfil(request);
    }
}
