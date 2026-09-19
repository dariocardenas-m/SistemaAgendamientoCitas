package com.piedrazul.especialidades;

public class EspecialidadDto {

    private Long id;
    private String nombre;
    private String descripcion;
    private String icono;
    private String color;
    private long profesionalesDisponibles;

    public EspecialidadDto(Especialidad e, long profesionalesDisponibles) {
        this.id = e.getId();
        this.nombre = e.getNombre();
        this.descripcion = e.getDescripcion();
        this.icono = e.getIcono();
        this.color = e.getColor();
        this.profesionalesDisponibles = profesionalesDisponibles;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getIcono() {
        return icono;
    }

    public String getColor() {
        return color;
    }

    public long getProfesionalesDisponibles() {
        return profesionalesDisponibles;
    }
}
