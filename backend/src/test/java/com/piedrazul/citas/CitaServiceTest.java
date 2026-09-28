package com.piedrazul.citas;

import com.piedrazul.Fixtures;
import com.piedrazul.config.*;
import com.piedrazul.medicos.*;
import com.piedrazul.pacientes.*;
import com.piedrazul.usuarios.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {
    @Mock CitaRepository citas;
    @Mock MedicoRepository medicos;
    @Mock PacienteRepository pacientes;
    @Mock ConfiguracionGlobalRepository configuracion;
    @InjectMocks CitaService service;
    Medico medico; Paciente paciente; LocalDate fecha;
    @BeforeEach void init() {
        medico=Fixtures.medico(); paciente=Fixtures.paciente(10); fecha=LocalDate.now().plusDays(1);
    }
    void reservaBase() {
        when(pacientes.findByUsuario(paciente.getUsuario())).thenReturn(Optional.of(paciente));
        when(medicos.lockById(1L)).thenReturn(Optional.of(medico));
    }
    @Test void ventanaPorDefectoEsCuatro() { assertThat(service.ventanaSemanas()).isEqualTo(4); }
    @Test void ventanaConfigurada() {
        ConfiguracionGlobal c=new ConfiguracionGlobal(); c.setVentanaSemanas(8);
        when(configuracion.findAll()).thenReturn(List.of(c));
        assertThat(service.ventanaSemanas()).isEqualTo(8);
    }
    @Test void fechasIncluyenSoloDiasConfiguradosDentroVentana() {
        medico.setDiasAtencion("LUN,VIE"); when(medicos.findById(1L)).thenReturn(Optional.of(medico));
        List<LocalDate> fechas=service.fechasHabiles(1L);
        assertThat(fechas).isNotEmpty().allSatisfy(d->{
            assertThat(d).isBetween(LocalDate.now(),LocalDate.now().plusWeeks(4));
            assertThat(d.getDayOfWeek()).isIn(DayOfWeek.MONDAY,DayOfWeek.FRIDAY);
        });
    }
    @Test void medicoInexistente() {
        assertThatThrownBy(()->service.fechasHabiles(1L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void disponibilidadExcluyeOcupadasYLiberaCanceladas() {
        when(medicos.findById(1L)).thenReturn(Optional.of(medico));
        Cita ocupada=Fixtures.cita(paciente,medico,fecha,LocalTime.of(8,0));
        Cita cancelada=Fixtures.cita(paciente,medico,fecha,LocalTime.of(8,30)); cancelada.setEstado(EstadoCita.CANCELADA);
        when(citas.findByMedicoIdAndFechaOrderByHoraAsc(1L,fecha)).thenReturn(List.of(ocupada,cancelada));
        DisponibilidadDto d=service.disponibilidad(1L,fecha);
        assertThat(d.getLibres()).containsExactly(LocalTime.of(8,30),LocalTime.of(9,0),LocalTime.of(9,30));
        assertThat(d.getOcupadas()).containsExactly(LocalTime.of(8,0));
    }
    @Test void noOfreceHorasPasadasHoy() {
        when(medicos.findById(1L)).thenReturn(Optional.of(medico));
        LocalTime antes=LocalTime.now();
        assertThat(service.disponibilidad(1L,LocalDate.now()).getLibres()).allMatch(h->h.isAfter(antes));
    }
    @ParameterizedTest @ValueSource(ints={15,30,45,60})
    void franjasCompletasNoExcedenCierre(int intervalo) {
        medico.setIntervaloMinutos(intervalo); when(medicos.findById(1L)).thenReturn(Optional.of(medico));
        var d=service.disponibilidad(1L,fecha);
        assertThat(d.getLibres()).hasSize(120/intervalo).allMatch(h->!h.plusMinutes(intervalo).isAfter(medico.getHoraCierre()));
    }
    @ParameterizedTest @ValueSource(ints={-1,29})
    void rechazaFechasFueraVentana(int dias) {
        when(medicos.findById(1L)).thenReturn(Optional.of(medico));
        assertThatThrownBy(()->service.disponibilidad(1L,LocalDate.now().plusDays(dias))).isInstanceOf(ReglaNegocioException.class);
    }
    @Test void rechazaDiaNoLaboral() {
        medico.setDiasAtencion(fecha.getDayOfWeek()==DayOfWeek.MONDAY?"MAR":"LUN");
        when(medicos.findById(1L)).thenReturn(Optional.of(medico));
        assertThatThrownBy(()->service.disponibilidad(1L,fecha)).isInstanceOf(ReglaNegocioException.class);
    }
    @Test void reservaPacienteIgnoraPacienteIdAjenoYUsaPerfilAutenticado() {
        reservaBase(); when(citas.save(any())).thenAnswer(i->i.getArgument(0));
        var r=Fixtures.reserva(fecha,LocalTime.of(8,0)); r.setPacienteId(999L); r.setModalidad(null);
        CitaDto dto=service.reservar(paciente.getUsuario(),r);
        assertThat(dto.getPacienteId()).isEqualTo(10); assertThat(dto.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(dto.getModalidad()).isEqualTo(ModalidadCita.PRESENCIAL);
        assertThat(dto.getCodigo()).matches("CITA-"+fecha.getYear()+"-\\d{4}");
        verify(pacientes,never()).findById(any());
        InOrder order=inOrder(medicos,citas); order.verify(medicos).lockById(1L);
        order.verify(citas).lockByMedicoAndFecha(1L,fecha); order.verify(citas).existsByCodigo(anyString()); order.verify(citas).save(any());
    }
    @Test void agendadorDebeIndicarPaciente() {
        assertThatThrownBy(()->service.reservar(Fixtures.usuario(20,Rol.AGENDADOR),Fixtures.reserva(fecha,LocalTime.of(8,0))))
            .isInstanceOf(ReglaNegocioException.class); verifyNoInteractions(citas,medicos);
    }
    @Test void agendadorReservaParaPacienteIndicado() {
        when(pacientes.findById(10L)).thenReturn(Optional.of(paciente));
        when(medicos.lockById(1L)).thenReturn(Optional.of(medico)); when(citas.save(any())).thenAnswer(i->i.getArgument(0));
        var r=Fixtures.reserva(fecha,LocalTime.of(8,0)); r.setPacienteId(10L);
        assertThat(service.reservar(Fixtures.usuario(20,Rol.AGENDADOR),r).getPacienteId()).isEqualTo(10);
    }
    @Test void noReservaHoraFueraDeFranja() {
        reservaBase(); assertThatThrownBy(()->service.reservar(paciente.getUsuario(),Fixtures.reserva(fecha,LocalTime.of(8,10))))
            .isInstanceOf(ReglaNegocioException.class); verify(citas,never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings={"08:00","08:15","07:45"})
    void rechazaSolapamientos(String existente) {
        reservaBase(); Cita c=Fixtures.cita(paciente,medico,fecha,LocalTime.parse(existente));
        when(citas.lockByMedicoAndFecha(1L,fecha)).thenReturn(List.of(c));
        assertThatThrownBy(()->service.reservar(paciente.getUsuario(),Fixtures.reserva(fecha,LocalTime.of(8,0))))
            .isInstanceOf(ConflictoReservaException.class); verify(citas,never()).save(any());
    }
    @Test void permiteFranjasContiguas() {
        reservaBase(); when(citas.lockByMedicoAndFecha(1L,fecha)).thenReturn(List.of(Fixtures.cita(paciente,medico,fecha,LocalTime.of(8,30))));
        when(citas.save(any())).thenAnswer(i->i.getArgument(0));
        assertThat(service.reservar(paciente.getUsuario(),Fixtures.reserva(fecha,LocalTime.of(8,0)))).isNotNull();
    }
    @Test void permiteReservarFranjaCancelada() {
        reservaBase(); Cita c=Fixtures.cita(paciente,medico,fecha,LocalTime.of(8,0)); c.setEstado(EstadoCita.CANCELADA);
        when(citas.lockByMedicoAndFecha(1L,fecha)).thenReturn(List.of(c)); when(citas.save(any())).thenAnswer(i->i.getArgument(0));
        assertThat(service.reservar(paciente.getUsuario(),Fixtures.reserva(fecha,LocalTime.of(8,0)))).isNotNull();
    }
    @Test void pacienteCancelaSoloSuCita() {
        Cita c=Fixtures.cita(paciente,medico,fecha,LocalTime.of(8,0));
        when(citas.findById(1L)).thenReturn(Optional.of(c)); when(pacientes.findByUsuario(paciente.getUsuario())).thenReturn(Optional.of(paciente));
        when(citas.save(c)).thenReturn(c);
        assertThat(service.cambiarEstado(paciente.getUsuario(),1L,EstadoCita.CANCELADA).getEstado()).isEqualTo(EstadoCita.CANCELADA);
    }
    @Test void noCancelaCitaAjena() {
        when(citas.findById(1L)).thenReturn(Optional.of(Fixtures.cita(Fixtures.paciente(99),medico,fecha,LocalTime.of(8,0))));
        when(pacientes.findByUsuario(paciente.getUsuario())).thenReturn(Optional.of(paciente));
        assertThatThrownBy(()->service.cambiarEstado(paciente.getUsuario(),1L,EstadoCita.CANCELADA))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class); verify(citas,never()).save(any());
    }
    @Test void pacienteNoMarcaAtendida() {
        when(citas.findById(1L)).thenReturn(Optional.of(Fixtures.cita(paciente,medico,fecha,LocalTime.of(8,0))));
        when(pacientes.findByUsuario(paciente.getUsuario())).thenReturn(Optional.of(paciente));
        assertThatThrownBy(()->service.cambiarEstado(paciente.getUsuario(),1L,EstadoCita.ATENDIDA)).isInstanceOf(ReglaNegocioException.class);
    }
    @Test void consultaAgendaValidaProfesional() {
        assertThatThrownBy(()->service.consultarAgenda(999L,fecha)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void consultaAgendaFiltraMedicoYFecha() {
        when(medicos.existsById(1L)).thenReturn(true);
        when(citas.findByMedicoIdAndFechaOrderByHoraAsc(1L,fecha)).thenReturn(List.of(Fixtures.cita(paciente,medico,fecha,LocalTime.of(8,0))));
        assertThat(service.consultarAgenda(1L,fecha)).extracting(CitaDto::getPacienteId).containsExactly(10L);
    }
    @Test void consultaPacienteUsaIdDelPerfil() {
        when(pacientes.findByUsuario(paciente.getUsuario())).thenReturn(Optional.of(paciente));
        assertThat(service.citasPaciente(paciente.getUsuario())).isEmpty(); verify(citas).findByPacienteIdOrderByFechaDescHoraDesc(10L);
    }
    @Test void citaInexistenteDevuelveError() { assertThatThrownBy(()->service.porId(paciente.getUsuario(),999L)).isInstanceOf(RecursoNoEncontradoException.class); }
}
