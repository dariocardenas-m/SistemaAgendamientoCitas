package com.piedrazul.medicos;

import java.time.LocalTime;
import java.util.List;

public class MedicoConfigRequest {

    private List<String> diasAtencion;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private Integer intervaloMinutos;

    public List<String> getDiasAtencion() {
        return diasAtencion;
    }

    public void setDiasAtencion(List<String> diasAtencion) {
        this.diasAtencion = diasAtencion;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime horaApertura) {
        this.horaApertura = horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(LocalTime horaCierre) {
        this.horaCierre = horaCierre;
    }

    public Integer getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public void setIntervaloMinutos(Integer intervaloMinutos) {
        this.intervaloMinutos = intervaloMinutos;
    }
}
