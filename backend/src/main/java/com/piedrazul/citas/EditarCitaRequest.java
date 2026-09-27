package com.piedrazul.citas;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class EditarCitaRequest {

    private Long medicoId;
    private LocalDate fecha;
    private LocalTime hora;
    private ModalidadCita modalidad;
    @Size(max = 500, message = "El motivo no puede superar 500 caracteres.")
    private String motivo;

    public Long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(Long medicoId) {
        this.medicoId = medicoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public ModalidadCita getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadCita modalidad) {
        this.modalidad = modalidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
