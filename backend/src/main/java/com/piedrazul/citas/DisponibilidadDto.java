package com.piedrazul.citas;

import java.time.LocalTime;
import java.util.List;

public class DisponibilidadDto {

    private List<LocalTime> libres;
    private List<LocalTime> ocupadas;
    private Integer intervaloMinutos;

    public DisponibilidadDto(List<LocalTime> libres, List<LocalTime> ocupadas, Integer intervaloMinutos) {
        this.libres = libres;
        this.ocupadas = ocupadas;
        this.intervaloMinutos = intervaloMinutos;
    }

    public List<LocalTime> getLibres() {
        return libres;
    }

    public List<LocalTime> getOcupadas() {
        return ocupadas;
    }

    public Integer getIntervaloMinutos() {
        return intervaloMinutos;
    }
}
