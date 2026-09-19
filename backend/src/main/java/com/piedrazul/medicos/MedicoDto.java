package com.piedrazul.medicos;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class MedicoDto {

    private Long id;
    private String nombreCompleto;
    private Long especialidadId;
    private String especialidad;
    private String consultorio;
    private String fotoUrl;
    private Integer aniosExperiencia;
    private Double rating;
    private String descripcion;
    private List<String> diasAtencion;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private Integer intervaloMinutos;

    public static MedicoDto from(Medico m) {
        MedicoDto dto = new MedicoDto();
        dto.id = m.getId();
        dto.nombreCompleto = m.getNombreCompleto();
        dto.especialidadId = m.getEspecialidad().getId();
        dto.especialidad = m.getEspecialidad().getNombre();
        dto.consultorio = m.getConsultorio();
        dto.fotoUrl = m.getFotoUrl();
        dto.aniosExperiencia = m.getAniosExperiencia();
        dto.rating = m.getRating();
        dto.descripcion = m.getDescripcion();
        dto.diasAtencion = Arrays.asList(m.getDiasAtencion().split(","));
        dto.horaApertura = m.getHoraApertura();
        dto.horaCierre = m.getHoraCierre();
        dto.intervaloMinutos = m.getIntervaloMinutos();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public Long getEspecialidadId() {
        return especialidadId;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getConsultorio() {
        return consultorio;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public Integer getAniosExperiencia() {
        return aniosExperiencia;
    }

    public Double getRating() {
        return rating;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public List<String> getDiasAtencion() {
        return diasAtencion;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public Integer getIntervaloMinutos() {
        return intervaloMinutos;
    }
}
