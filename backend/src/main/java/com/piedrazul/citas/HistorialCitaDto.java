package com.piedrazul.citas;

import java.time.LocalDate;

/**
 * Vista mínima del historial clínico para evitar exponer datos de contacto,
 * notas de consulta u otra información que no es necesaria en la búsqueda.
 */
public record HistorialCitaDto(LocalDate fecha, String medico, String especialidad, EstadoCita estado) {

    public static HistorialCitaDto from(Cita cita) {
        return new HistorialCitaDto(
                cita.getFecha(),
                cita.getMedico().getNombreCompleto(),
                cita.getMedico().getEspecialidad().getNombre(),
                cita.getEstado());
    }
}
