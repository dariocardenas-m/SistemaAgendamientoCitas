package com.piedrazul;

import com.piedrazul.citas.*;
import com.piedrazul.medicos.Medico;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.especialidades.Especialidad;
import com.piedrazul.usuarios.*;
import java.time.*;

public final class Fixtures {
    private Fixtures() {}
    public static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario(); u.setId(id); u.setRol(rol);
        u.setNombreCompleto("Usuario de prueba"); u.setEmail("u"+id+"@example.test");
        u.setPasswordHash("hash-de-prueba"); return u;
    }
    public static Paciente paciente(long id) {
        Paciente p = new Paciente(); p.setId(id); p.setUsuario(usuario(id,Rol.PACIENTE));
        p.setDocumento("DOC"+id); p.setTelefono("3000000000"); p.setCodigo("PAC-"+id); return p;
    }
    public static Medico medico() {
        Especialidad e = new Especialidad(); e.setId(1L); e.setNombre("General");
        Medico m = new Medico(); m.setId(1L); m.setNombreCompleto("Profesional prueba");
        m.setEspecialidad(e); m.setDiasAtencion("LUN,MAR,MIE,JUE,VIE,SAB,DOM");
        m.setHoraApertura(LocalTime.of(8,0)); m.setHoraCierre(LocalTime.of(10,0));
        m.setIntervaloMinutos(30); return m;
    }
    public static Cita cita(Paciente p, Medico m, LocalDate fecha, LocalTime hora) {
        Cita c = new Cita(); c.setId(1L); c.setCodigo("CITA-PRUEBA"); c.setPaciente(p);
        c.setMedico(m); c.setFecha(fecha); c.setHora(hora); c.setDuracionMinutos(30); return c;
    }
    public static ReservarCitaRequest reserva(LocalDate fecha, LocalTime hora) {
        ReservarCitaRequest r = new ReservarCitaRequest(); r.setMedicoId(1L);
        r.setFecha(fecha); r.setHora(hora); return r;
    }
}
