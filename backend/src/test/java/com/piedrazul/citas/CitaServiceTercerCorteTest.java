package com.piedrazul.citas;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTercerCorteTest {

    @Mock CitaRepository citaRepository;
    @Mock MedicoRepository medicoRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock ConfiguracionGlobalRepository configuracionRepository;
    @Mock SecurityAuditLogger securityAuditLogger;

    private CitaService service;
    private Usuario pacienteUsuario;
    private Paciente paciente;
    private Cita cita;

    @BeforeEach
    void setUp() {
        service = new CitaService(citaRepository, medicoRepository, pacienteRepository,
                configuracionRepository, securityAuditLogger);
        pacienteUsuario = new Usuario();
        pacienteUsuario.setId(11L);
        pacienteUsuario.setRol(Rol.PACIENTE);
        paciente = new Paciente();
        paciente.setId(21L);
        paciente.setUsuario(pacienteUsuario);
        paciente.setDocumento("1000000001");
        cita = cita(paciente, medico(31L, "Dra. Laura Martínez"), LocalDate.now().plusDays(2));
        lenient().when(citaRepository.lockById(51L)).thenReturn(Optional.of(cita));
        lenient().when(pacienteRepository.findByUsuario(pacienteUsuario)).thenReturn(Optional.of(paciente));
    }

    @Test
    void cancelaCitaPropiaEnElLimiteDeDosDiasYGuardaLaJustificacion() {
        when(citaRepository.save(cita)).thenReturn(cita);
        CancelarCitaRequest request = new CancelarCitaRequest();
        request.setJustificacion("  No podré asistir  ");

        CitaDto resultado = service.cancelar(pacienteUsuario, 51L, request);

        assertEquals(EstadoCita.CANCELADA, resultado.getEstado());
        assertEquals("No podré asistir", resultado.getJustificacionCancelacion());
        verify(citaRepository).save(cita);
    }

    @Test
    void rechazaCancelacionConMenosDeDosDiasCalendario() {
        cita.setFecha(LocalDate.now().plusDays(1));
        CancelarCitaRequest request = request("Cambio de planes");

        assertThrows(ReglaNegocioException.class, () -> service.cancelar(pacienteUsuario, 51L, request));

        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
        org.junit.jupiter.api.Assertions.assertNull(cita.getJustificacionCancelacion());
    }

    @Test
    void rechazaJustificacionEnBlanco() {
        CancelarCitaRequest request = request("   \n ");

        assertThrows(ReglaNegocioException.class, () -> service.cancelar(pacienteUsuario, 51L, request));

        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
    }

    @Test
    void repetirCancelacionNoCambiaUnaCitaYaCancelada() {
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setJustificacionCancelacion("Primera justificación");

        assertThrows(ReglaNegocioException.class,
                () -> service.cancelar(pacienteUsuario, 51L, request("Intento repetido")));

        assertEquals("Primera justificación", cita.getJustificacionCancelacion());
    }

    @Test
    void pacienteNoPuedeCancelarLaCitaDeOtraPersona() {
        Paciente propietario = new Paciente();
        propietario.setId(99L);
        cita.setPaciente(propietario);

        assertThrows(AccessDeniedException.class,
                () -> service.cancelar(pacienteUsuario, 51L, request("Motivo")));

        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
        verify(securityAuditLogger).accesoDenegado(pacienteUsuario, "CANCELAR_CITA", 31L);
    }

    @Test
    void reporteAgrupaPorMedicoMesYEstadoYCompletaLosMesesSinDatos() {
        Usuario agendador = new Usuario();
        agendador.setId(41L);
        agendador.setRol(Rol.AGENDADOR);
        Medico laura = medico(31L, "Dra. Laura Martínez");
        when(citaRepository.reporteAnual(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), null))
                .thenReturn(List.of(
                        fila(31L, "Dra. Laura Martínez", 1, EstadoCita.CONFIRMADA, 2L),
                        fila(31L, "Dra. Laura Martínez", 1, EstadoCita.ATENDIDA, 1L),
                        fila(31L, "Dra. Laura Martínez", 3, EstadoCita.CANCELADA, 4L)));
        when(medicoRepository.findAllByOrderByNombreCompletoAsc()).thenReturn(List.of(laura));

        ReporteAnualCitasDto reporte = service.reporteAnual(agendador, 2026);

        assertEquals(2026, reporte.anio());
        assertEquals(1, reporte.medicos().size());
        assertEquals(12, reporte.medicos().get(0).meses().size());
        assertEquals(2, reporte.medicos().get(0).meses().get(0).confirmadas());
        assertEquals(1, reporte.medicos().get(0).meses().get(0).atendidas());
        assertEquals(4, reporte.medicos().get(0).meses().get(2).canceladas());
        assertEquals(0, reporte.medicos().get(0).meses().get(1).confirmadas());
        verify(securityAuditLogger).consultaExitosa(agendador, "REPORTE_ANUAL_CITAS");
    }

    @Test
    void reporteDeMedicoSeLimitaASuPerfil() {
        Usuario medicoUsuario = new Usuario();
        medicoUsuario.setId(61L);
        medicoUsuario.setRol(Rol.MEDICO_TERAPISTA);
        Medico laura = medico(31L, "Dra. Laura Martínez");
        when(medicoRepository.findByUsuario(medicoUsuario)).thenReturn(Optional.of(laura));
        when(citaRepository.reporteAnual(eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2027, 1, 1)), eq(31L)))
                .thenReturn(List.<Object[]>of(fila(31L, "Dra. Laura Martínez", 6, EstadoCita.ATENDIDA, 3L)));
        when(medicoRepository.findAllByOrderByNombreCompletoAsc()).thenReturn(List.of(laura));

        ReporteAnualCitasDto reporte = service.reporteAnual(medicoUsuario, 2026);

        assertEquals(List.of(31L), reporte.medicos().stream().map(SerieMedicoReporteDto::medicoId).toList());
        verify(citaRepository).reporteAnual(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 31L);
    }

    @Test
    void rechazaAnioFueraDelRangoPermitido() {
        Usuario agendador = new Usuario();
        agendador.setRol(Rol.AGENDADOR);

        assertThrows(ReglaNegocioException.class, () -> service.reporteAnual(agendador, 1800));
    }

    private CancelarCitaRequest request(String justificacion) {
        CancelarCitaRequest request = new CancelarCitaRequest();
        request.setJustificacion(justificacion);
        return request;
    }

    private Object[] fila(Long medicoId, String medicoNombre, int mes, EstadoCita estado, Long total) {
        return new Object[]{medicoId, medicoNombre, mes, estado, total};
    }

    private Cita cita(Paciente paciente, Medico medico, LocalDate fecha) {
        Cita nueva = new Cita();
        nueva.setId(51L);
        nueva.setCodigo("CITA-TEST-51");
        nueva.setPaciente(paciente);
        nueva.setMedico(medico);
        nueva.setFecha(fecha);
        nueva.setEstado(EstadoCita.CONFIRMADA);
        return nueva;
    }

    private Medico medico(Long id, String nombre) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Medicina general");
        Medico medico = new Medico();
        medico.setId(id);
        medico.setNombreCompleto(nombre);
        medico.setEspecialidad(especialidad);
        return medico;
    }
}
