package com.piedrazul;
import com.piedrazul.citas.*;
import com.piedrazul.config.ConfiguracionGlobal;
import com.piedrazul.medicos.*;
import com.piedrazul.pacientes.*;
import com.piedrazul.especialidades.*;
import com.piedrazul.usuarios.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

// Entidades anemicas: verificamos invariantes iniciales y proyecciones, no un test por getter/setter.
class DominioTest {
    @Test void citaNuevaTieneEstadoModalidadYFechaDeCreacion() {
        LocalDateTime antes=LocalDateTime.now(); Cita c=new Cita();
        assertThat(c.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(c.getModalidad()).isEqualTo(ModalidadCita.PRESENCIAL);
        assertThat(c.getCreadaEn()).isBetween(antes,LocalDateTime.now());
    }
    @Test void citaDtoConservaAsociacionesDePacienteYProfesional() {
        Cita c=Fixtures.cita(Fixtures.paciente(12),Fixtures.medico(),LocalDate.of(2027,1,5),LocalTime.of(8,0));
        CitaDto dto=CitaDto.from(c); assertThat(dto.getPacienteId()).isEqualTo(12);
        assertThat(dto.getMedicoId()).isEqualTo(1); assertThat(dto.getEspecialidad()).isEqualTo("General");
        assertThat(dto.getFecha()).isEqualTo(c.getFecha()); assertThat(dto.getDuracionMinutos()).isEqualTo(30);
    }
    @Test void usuarioNuevoActivoYDtoNoExponeHash() throws Exception {
        Usuario u=Fixtures.usuario(2,Rol.PACIENTE); assertThat(u.isActivo()).isTrue();
        String json=new ObjectMapper().writeValueAsString(UsuarioDto.from(u));
        assertThat(json).doesNotContain("password","hash-de-prueba").contains("PACIENTE");
    }
    @Test void pacienteDtoMantieneIdentidadDelUsuario() throws Exception {
        Paciente p=Fixtures.paciente(4); String json=new ObjectMapper().writeValueAsString(PacienteDto.from(p));
        assertThat(json).contains("DOC4","PAC-4","u4@example.test").doesNotContain("hash-de-prueba");
    }
    @Test void medicoDtoConservaConfiguracionDeAgenda() throws Exception {
        Medico m=Fixtures.medico(); ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
        String json=mapper.writeValueAsString(MedicoDto.from(m));
        assertThat(json).contains("General","intervaloMinutos","30","diasAtencion");
    }
    @Test void especialidadDtoConservaCatalogo() throws Exception {
        Especialidad e=Fixtures.medico().getEspecialidad(); e.setDescripcion("Atencion primaria");
        assertThat(new ObjectMapper().writeValueAsString(new EspecialidadDto(e,2))).contains("General","Atencion primaria");
    }
    @Test void configuracionNuevaAbreCuatroSemanas() { assertThat(new ConfiguracionGlobal().getVentanaSemanas()).isEqualTo(4); }
}
