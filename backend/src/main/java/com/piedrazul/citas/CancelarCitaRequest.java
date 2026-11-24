package com.piedrazul.citas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelarCitaRequest {

    @NotBlank(message = "La justificación de cancelación es obligatoria.")
    @Size(max = 500, message = "La justificación no puede superar 500 caracteres.")
    private String justificacion;

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }
}
