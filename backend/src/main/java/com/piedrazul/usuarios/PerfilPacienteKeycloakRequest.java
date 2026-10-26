package com.piedrazul.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilPacienteKeycloakRequest(
        @NotBlank @Size(max = 120) String nombreCompleto,
        @NotBlank @Pattern(regexp = "\\d{6,15}", message = "El documento debe contener entre 6 y 15 dígitos.") String documento,
        @NotBlank @Size(max = 30) String telefono) {
}
