package com.piedrazul.citas;

import java.time.LocalDate;
import java.time.LocalTime;

public class CitaDto {

    private Long id;
    private String codigo;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String pacienteTelefono;
    private String pacienteEmail;
    private String pacienteCodigo;
    private Long medicoId;
    private String medicoNombre;
    private String especialidad;
    private String consultorio;
    private String medicoFoto;
    private LocalDate fecha;
    private LocalTime hora;
    private Integer duracionMinutos;
    private ModalidadCita modalidad;
    private String motivo;
    private EstadoCita estado;

    public static CitaDto from(Cita c) {
        CitaDto dto = new CitaDto();
        dto.id = c.getId();
        dto.codigo = c.getCodigo();
        dto.pacienteId = c.getPaciente().getId();
        dto.pacienteNombre = c.getPaciente().getUsuario().getNombreCompleto();
        dto.pacienteDocumento = c.getPaciente().getDocumento();
        dto.pacienteTelefono = c.getPaciente().getTelefono();
        dto.pacienteEmail = c.getPaciente().getUsuario().getEmail();
        dto.pacienteCodigo = c.getPaciente().getCodigo();
        dto.medicoId = c.getMedico().getId();
        dto.medicoNombre = c.getMedico().getNombreCompleto();
        dto.especialidad = c.getMedico().getEspecialidad().getNombre();
        dto.consultorio = c.getMedico().getConsultorio();
        dto.medicoFoto = c.getMedico().getFotoUrl();
        dto.fecha = c.getFecha();
        dto.hora = c.getHora();
        dto.duracionMinutos = c.getDuracionMinutos();
        dto.modalidad = c.getModalidad();
        dto.motivo = c.getMotivo();
        dto.estado = c.getEstado();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public String getPacienteNombre() {
        return pacienteNombre;
    }

    public String getPacienteDocumento() {
        return pacienteDocumento;
    }

    public String getPacienteTelefono() {
        return pacienteTelefono;
    }

    public String getPacienteEmail() {
        return pacienteEmail;
    }

    public String getPacienteCodigo() {
        return pacienteCodigo;
    }

    public Long getMedicoId() {
        return medicoId;
    }

    public String getMedicoNombre() {
        return medicoNombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getConsultorio() {
        return consultorio;
    }

    public String getMedicoFoto() {
        return medicoFoto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public ModalidadCita getModalidad() {
        return modalidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public EstadoCita getEstado() {
        return estado;
    }
}
