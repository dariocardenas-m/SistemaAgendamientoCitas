package com.piedrazul.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilBasicoKeycloakRequest(@NotBlank @Size(max = 120) String nombreCompleto) {
}
