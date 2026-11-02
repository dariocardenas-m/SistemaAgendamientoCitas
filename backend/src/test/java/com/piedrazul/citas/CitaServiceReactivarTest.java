package com.piedrazul.citas;

import com.piedrazul.config.ConflictoReservaException;
import com.piedrazul.config.ConfiguracionGlobalRepository;
import com.piedrazul.config.SecurityAuditLogger;
import com.piedrazul.especialidades.Especialidad;
import com.piedrazul.medicos.Medico;
import com.piedrazul.medicos.MedicoRepository;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.pacientes.PacienteRepository;
import com.piedrazul.usuarios.Rol;
import com.piedrazul.usuarios.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceReactivarTest {
    @Mock CitaRepository citaRepository;
    @Mock MedicoRepository medicoRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock ConfiguracionGlobalRepository configuracionRepository;
    @Mock SecurityAuditLogger securityAuditLogger;
    @InjectMocks CitaService service;
    Usuario admin;
    Medico medico;
    Paciente paciente;
    Cita cancelada;

    @BeforeEach
    void preparar() {
        admin = new Usuario(); admin.setId(1L); admin.setRol(Rol.ADMINISTRADOR);
        Usuario usuario = new Usuario(); usuario.setId(2L); usuario.setNombreCompleto("Paciente de prueba");
        paciente = new Paciente(); paciente.setId(2L); paciente.setUsuario(usuario);
        Especialidad especialidad = new Especialidad(); especialidad.setNombre("Medicina general");
        medico = new Medico(); medico.setId(3L); medico.setEspecialidad(especialidad);
        cancelada = cita(10L, LocalTime.of(8, 0), EstadoCita.CANCELADA);
        when(citaRepository.findById(10L)).thenReturn(Optional.of(cancelada));
        lenient().when(medicoRepository.lockById(3L)).thenReturn(Optional.of(medico));
        lenient().when(citaRepository.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void reactivarNoSolapaLaDuracionDeOtraCitaActiva() {
        Cita ocupada = cita(11L, LocalTime.of(8, 15), EstadoCita.CONFIRMADA);
        lenient().when(citaRepository.lockByMedicoAndFecha(3L, cancelada.getFecha()))
                .thenReturn(List.of(cancelada, ocupada));
        assertThrows(ConflictoReservaException.class,
                () -> service.cambiarEstado(admin, 10L, EstadoCita.CONFIRMADA));
        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void unaCitaAdyacenteNoImpideReactivarLaFranjaLibre() {
        Cita siguiente = cita(11L, LocalTime.of(8, 30), EstadoCita.CONFIRMADA);
        lenient().when(citaRepository.lockByMedicoAndFecha(3L, cancelada.getFecha()))
                .thenReturn(List.of(cancelada, siguiente));
        CitaDto result = service.cambiarEstado(admin, 10L, EstadoCita.CONFIRMADA);
        assertEquals(EstadoCita.CONFIRMADA, result.getEstado());
        assertEquals(LocalTime.of(8, 0), result.getHora());
    }

    private Cita cita(Long id, LocalTime hora, EstadoCita estado) {
        Cita cita = new Cita(); cita.setId(id); cita.setCodigo("AUD-" + id);
        cita.setPaciente(paciente); cita.setMedico(medico);
        cita.setFecha(LocalDate.now().plusDays(1)); cita.setHora(hora);
        cita.setDuracionMinutos(30); cita.setModalidad(ModalidadCita.PRESENCIAL); cita.setEstado(estado);
        return cita;
    }
}
