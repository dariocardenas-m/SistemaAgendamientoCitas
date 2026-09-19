package com.piedrazul.pacientes;

public class PacienteDto {

    private Long id;
    private String codigo;
    private String nombreCompleto;
    private String documento;
    private String telefono;
    private String email;
    private String perfilClinico;

    public static PacienteDto from(Paciente p) {
        PacienteDto dto = new PacienteDto();
        dto.id = p.getId();
        dto.codigo = p.getCodigo();
        dto.nombreCompleto = p.getUsuario().getNombreCompleto();
        dto.documento = p.getDocumento();
        dto.telefono = p.getTelefono();
        dto.email = p.getUsuario().getEmail();
        dto.perfilClinico = p.getPerfilClinico();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getPerfilClinico() {
        return perfilClinico;
    }
}
