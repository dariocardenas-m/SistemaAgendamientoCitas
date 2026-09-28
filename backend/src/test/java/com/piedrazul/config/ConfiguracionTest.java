package com.piedrazul.config;
import com.piedrazul.Fixtures;
import com.piedrazul.medicos.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ConfiguracionTest {
    ConfiguracionGlobalRepository cfg=mock(ConfiguracionGlobalRepository.class);
    ConfiguracionController controller=new ConfiguracionController(cfg);
    MedicoRepository medicos=mock(MedicoRepository.class);
    MedicoController medicoController=new MedicoController(medicos);
    MedicoConfigRequest solicitud() {
        var r=new MedicoConfigRequest(); r.setDiasAtencion(List.of("LUN","MAR")); r.setHoraApertura(LocalTime.of(8,0));
        r.setHoraCierre(LocalTime.of(12,0)); r.setIntervaloMinutos(30); return r;
    }
    @ParameterizedTest @ValueSource(ints={1,4,12}) void aceptaVentanaEnLimites(int valor) {
        assertThat(controller.actualizar(Map.of("ventanaSemanas",valor))).containsEntry("ventanaSemanas",valor); verify(cfg).save(any());
    }
    @ParameterizedTest @ValueSource(ints={-1,0,13}) void rechazaVentanaInvalida(int valor) {
        assertThatThrownBy(()->controller.actualizar(Map.of("ventanaSemanas",valor))).isInstanceOf(ReglaNegocioException.class);
        verify(cfg,never()).save(any());
    }
    @Test void rechazaVentanaAusente() { assertThatThrownBy(()->controller.actualizar(Map.of())).isInstanceOf(ReglaNegocioException.class); }
    @Test void creaConfiguracionInicialCuandoFalta() {
        when(cfg.save(any())).thenAnswer(i->i.getArgument(0));
        assertThat(controller.obtener()).containsEntry("ventanaSemanas",4);
    }
    @ParameterizedTest @ValueSource(ints={15,30,45,60}) void aceptaIntervalosSoportados(int minutos) {
        when(medicos.findById(1L)).thenReturn(Optional.of(Fixtures.medico())); when(medicos.save(any())).thenAnswer(i->i.getArgument(0));
        var r=solicitud(); r.setIntervaloMinutos(minutos); medicoController.actualizarConfig(1L,r);
        verify(medicos).save(argThat(m->m.getIntervaloMinutos()==minutos && m.getDiasAtencion().equals("LUN,MAR")));
    }
    @ParameterizedTest @ValueSource(ints={0,-15,20,120}) void rechazaIntervalosInvalidos(int minutos) {
        when(medicos.findById(1L)).thenReturn(Optional.of(Fixtures.medico())); var r=solicitud(); r.setIntervaloMinutos(minutos);
        assertThatThrownBy(()->medicoController.actualizarConfig(1L,r)).isInstanceOf(ReglaNegocioException.class); verify(medicos,never()).save(any());
    }
    @Test void rechazaDiasVacios() {
        when(medicos.findById(1L)).thenReturn(Optional.of(Fixtures.medico())); var r=solicitud(); r.setDiasAtencion(List.of());
        assertThatThrownBy(()->medicoController.actualizarConfig(1L,r)).isInstanceOf(ReglaNegocioException.class);
    }
    @Test void rechazaDiaDesconocido() {
        when(medicos.findById(1L)).thenReturn(Optional.of(Fixtures.medico())); var r=solicitud(); r.setDiasAtencion(List.of("XYZ"));
        assertThatThrownBy(()->medicoController.actualizarConfig(1L,r)).isInstanceOf(ReglaNegocioException.class);
    }
    @ParameterizedTest @ValueSource(strings={"07:00","08:00"}) void cierreDebeSerPosterior(String hora) {
        when(medicos.findById(1L)).thenReturn(Optional.of(Fixtures.medico())); var r=solicitud(); r.setHoraCierre(LocalTime.parse(hora));
        assertThatThrownBy(()->medicoController.actualizarConfig(1L,r)).isInstanceOf(ReglaNegocioException.class);
    }
}
