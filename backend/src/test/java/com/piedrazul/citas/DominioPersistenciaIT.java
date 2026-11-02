package com.piedrazul.citas;

import com.piedrazul.config.ConfiguracionGlobal;
import com.piedrazul.especialidades.Especialidad;
import com.piedrazul.medicos.Medico;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.usuarios.*;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** Contratos de persistencia de las entidades; no son pruebas de getters/setters. */
@DataJpaTest(properties={"spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop"})
class DominioPersistenciaIT {
    @Autowired TestEntityManager em;
    private Usuario usuario(String email){Usuario u=new Usuario();u.setNombreCompleto("Persona de prueba");u.setEmail(email);u.setPasswordHash("hash-ficticio");u.setRol(Rol.PACIENTE);return em.persistAndFlush(u);}
    private Paciente paciente(Usuario u,String doc,String codigo){Paciente p=new Paciente();p.setUsuario(u);p.setDocumento(doc);p.setCodigo(codigo);p.setTelefono("3000000000");return em.persistAndFlush(p);}
    private Especialidad especialidad(){Especialidad e=new Especialidad();e.setNombre("Medicina de prueba");return em.persistAndFlush(e);}
    private Medico medico(Especialidad e){Medico m=new Medico();m.setNombreCompleto("Profesional de prueba");m.setEspecialidad(e);m.setDiasAtencion("MONDAY,TUESDAY");m.setHoraApertura(LocalTime.of(8,0));m.setHoraCierre(LocalTime.of(16,0));m.setIntervaloMinutos(30);return em.persistAndFlush(m);}
    private Cita cita(Paciente p,Medico m,String codigo){Cita c=new Cita();c.setCodigo(codigo);c.setPaciente(p);c.setMedico(m);c.setFecha(LocalDate.of(2026,11,3));c.setHora(LocalTime.of(8,0));c.setDuracionMinutos(30);return em.persistAndFlush(c);}
    @Test void conservaRelacionesYProyectaUnaCitaDesdeLaBase(){
        Usuario u=usuario("uno@example.test");Paciente p=paciente(u,"12345678","PAC-TEST");Medico m=medico(especialidad());Cita c=cita(p,m,"CITA-TEST");
        ConfiguracionGlobal config=new ConfiguracionGlobal();config.setVentanaSemanas(6);em.persistAndFlush(config);Long id=c.getId(),cfg=config.getId();em.clear();
        Cita leida=em.find(Cita.class,id);CitaDto dto=CitaDto.from(leida);
        assertEquals("uno@example.test",dto.getPacienteEmail());assertEquals("12345678",dto.getPacienteDocumento());
        assertEquals("Medicina de prueba",dto.getEspecialidad());assertEquals(EstadoCita.CONFIRMADA,dto.getEstado());
        assertEquals(ModalidadCita.PRESENCIAL,dto.getModalidad());assertEquals(6,em.find(ConfiguracionGlobal.class,cfg).getVentanaSemanas());
    }
    @Test void correoDeUsuarioEsUnicoEnLaBase(){usuario("uno@example.test");assertThrows(PersistenceException.class,()->usuario("uno@example.test"));}
    @Test void documentoDePacienteEsUnicoAunqueCambieLaCuenta(){Paciente p=paciente(usuario("uno@example.test"),"12345678","PAC-UNO");Usuario otro=usuario("dos@example.test");assertThrows(PersistenceException.class,()->paciente(otro,p.getDocumento(),"PAC-DOS"));}
    @Test void unaCuentaNoPuedeTenerDosPerfilesPaciente(){Usuario u=usuario("uno@example.test");paciente(u,"12345678","PAC-UNO");assertThrows(PersistenceException.class,()->paciente(u,"87654321","PAC-DOS"));}
    @Test void especialidadDuplicadaSeRechaza(){especialidad();assertThrows(PersistenceException.class,this::especialidad);}
    @Test void medicoRequiereUnaEspecialidadPersistida(){assertThrows(PersistenceException.class,()->medico(null));}
    @Test void codigoDeCitaEsUnicoAunqueLaHoraCambie(){Paciente p=paciente(usuario("uno@example.test"),"12345678","PAC-UNO");Medico m=medico(especialidad());cita(p,m,"CITA-UNO");assertThrows(PersistenceException.class,()->cita(p,m,"CITA-UNO"));}
    @Test void configuracionNoPermiteVentanaNula(){ConfiguracionGlobal c=new ConfiguracionGlobal();c.setVentanaSemanas(null);assertThrows(PersistenceException.class,()->em.persistAndFlush(c));}
}
