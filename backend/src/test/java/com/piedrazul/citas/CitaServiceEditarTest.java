package com.piedrazul.citas;

import com.piedrazul.config.ConflictoReservaException;
import com.piedrazul.config.ConfiguracionGlobal;
import com.piedrazul.config.ConfiguracionGlobalRepository;
import com.piedrazul.config.ReglaNegocioException;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceEditarTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private ConfiguracionGlobalRepository configuracionRepository;
    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @InjectMocks
    private CitaService citaService;

    private Usuario pacienteActor;
    private Paciente paciente;
    private Medico medico;
    private Cita cita;
    private LocalDate fechaHabil;

    @BeforeEach
    void setUp() {
        ConfiguracionGlobal cfg = new ConfiguracionGlobal();
        cfg.setVentanaSemanas(8);
        lenient().when(configuracionRepository.findAll()).thenReturn(List.of(cfg));

        pacienteActor = usuario(1L, Rol.PACIENTE);
        paciente = paciente(10L, pacienteActor);
        medico = medico(5L);
        fechaHabil = siguienteHabil();
        cita = cita(100L, paciente, medico, fechaHabil, LocalTime.of(8, 0), EstadoCita.CONFIRMADA);
    }

    @Test
    void editarValidaActualizaFechaHoraYMotivo() {
        when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(pacienteRepository.findByUsuario(pacienteActor)).thenReturn(Optional.of(paciente));
        when(medicoRepository.lockById(5L)).thenReturn(Optional.of(medico));
        when(citaRepository.lockByMedicoAndFecha(5L, fechaHabil)).thenReturn(List.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        EditarCitaRequest request = request(5L, fechaHabil, LocalTime.of(9, 0), ModalidadCita.TELEMEDICINA, "Control");
        CitaDto dto = citaService.editar(pacienteActor, 100L, request);

        assertEquals(LocalTime.of(9, 0), dto.getHora());
        assertEquals(ModalidadCita.TELEMEDICINA, dto.getModalidad());
        assertEquals("Control", dto.getMotivo());
        assertEquals(EstadoCita.CONFIRMADA, dto.getEstado());
    }

    @Test
    void editarConHorarioOcupadoLanzaConflicto() {
        Cita ocupada = cita(101L, paciente, medico, fechaHabil, LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(pacienteRepository.findByUsuario(pacienteActor)).thenReturn(Optional.of(paciente));
        when(medicoRepository.lockById(5L)).thenReturn(Optional.of(medico));
        when(citaRepository.lockByMedicoAndFecha(5L, fechaHabil)).thenReturn(List.of(cita, ocupada));

        EditarCitaRequest request = request(5L, fechaHabil, LocalTime.of(9, 0), ModalidadCita.PRESENCIAL, "Control");

        assertThrows(ConflictoReservaException.class, () -> citaService.editar(pacienteActor, 100L, request));
    }

    @Test
    void editarCitaCanceladaLanzaReglaNegocio() {
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(pacienteRepository.findByUsuario(pacienteActor)).thenReturn(Optional.of(paciente));

        EditarCitaRequest request = request(5L, fechaHabil, LocalTime.of(9, 0), null, null);

        assertThrows(ReglaNegocioException.class, () -> citaService.editar(pacienteActor, 100L, request));
    }

    @Test
    void editarPorPacienteQueNoEsDuenoLanzaAccesoDenegado() {
        Usuario otroActor = usuario(2L, Rol.PACIENTE);
        Paciente otro = paciente(11L, otroActor);
        when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(pacienteRepository.findByUsuario(otroActor)).thenReturn(Optional.of(otro));

        EditarCitaRequest request = request(5L, fechaHabil, LocalTime.of(9, 0), null, null);

        assertThrows(AccessDeniedException.class, () -> citaService.editar(otroActor, 100L, request));
    }

    private EditarCitaRequest request(Long medicoId, LocalDate fecha, LocalTime hora, ModalidadCita modalidad, String motivo) {
        EditarCitaRequest r = new EditarCitaRequest();
        r.setMedicoId(medicoId);
        r.setFecha(fecha);
        r.setHora(hora);
        r.setModalidad(modalidad);
        r.setMotivo(motivo);
        return r;
    }

    private LocalDate siguienteHabil() {
        LocalDate d = LocalDate.now().plusDays(1);
        while (d.getDayOfWeek().getValue() > 5) {
            d = d.plusDays(1);
        }
        return d;
    }

    private Usuario usuario(Long id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombreCompleto("Usuario " + id);
        u.setEmail("u" + id + "@mail.com");
        u.setPasswordHash("x");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private Paciente paciente(Long id, Usuario usuario) {
        Paciente p = new Paciente();
        p.setId(id);
        p.setUsuario(usuario);
        p.setDocumento("100" + id);
        p.setTelefono("300");
        p.setCodigo("PAC-" + id);
        return p;
    }

    private Medico medico(Long id) {
        Especialidad esp = new Especialidad();
        esp.setId(1L);
        esp.setNombre("Medicina General");
        Medico m = new Medico();
        m.setId(id);
        m.setNombreCompleto("Dra. Laura Martínez");
        m.setEspecialidad(esp);
        m.setConsultorio("201");
        m.setDiasAtencion("LUN,MAR,MIE,JUE,VIE,SAB,DOM");
        m.setHoraApertura(LocalTime.of(8, 0));
        m.setHoraCierre(LocalTime.of(12, 0));
        m.setIntervaloMinutos(30);
        return m;
    }

    private Cita cita(Long id, Paciente p, Medico m, LocalDate fecha, LocalTime hora, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(id);
        c.setCodigo("CITA-TEST-" + id);
        c.setPaciente(p);
        c.setMedico(m);
        c.setFecha(fecha);
        c.setHora(hora);
        c.setDuracionMinutos(30);
        c.setModalidad(ModalidadCita.PRESENCIAL);
        c.setMotivo("Inicial");
        c.setEstado(estado);
        return c;
    }
}
