package com.piedrazul.citas;

import com.piedrazul.config.SeguridadUtil;
import com.piedrazul.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;
    private final SeguridadUtil seguridadUtil;

    public CitaController(CitaService citaService, SeguridadUtil seguridadUtil) {
        this.citaService = citaService;
        this.seguridadUtil = seguridadUtil;
    }

    @GetMapping("/fechas-habiles")
    public List<LocalDate> fechasHabiles(@RequestParam Long medicoId) {
        return citaService.fechasHabiles(medicoId);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadDto disponibilidad(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citaService.disponibilidad(medicoId, fecha);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PACIENTE','AGENDADOR','ADMINISTRADOR')")
    public CitaDto reservar(@Valid @RequestBody ReservarCitaRequest request) {
        return citaService.reservar(seguridadUtil.usuarioActual(), request);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<CitaDto> mias() {
        return citaService.citasPaciente(seguridadUtil.usuarioActual());
    }

    @GetMapping("/consulta")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR','MEDICO_TERAPISTA')")
    public List<CitaDto> consulta(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citaService.consultarAgenda(medicoId, fecha);
    }

    @GetMapping("/{id}")
    public CitaDto porId(@PathVariable Long id) {
        return citaService.porId(id);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('PACIENTE','AGENDADOR','ADMINISTRADOR')")
    public CitaDto cancelar(@PathVariable Long id) {
        return citaService.cambiarEstado(seguridadUtil.usuarioActual(), id, EstadoCita.CANCELADA);
    }

    @PatchMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR','MEDICO_TERAPISTA')")
    public CitaDto atender(@PathVariable Long id) {
        Usuario actor = seguridadUtil.usuarioActual();
        return citaService.cambiarEstado(actor, id, EstadoCita.ATENDIDA);
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public CitaDto reactivar(@PathVariable Long id) {
        return citaService.cambiarEstado(seguridadUtil.usuarioActual(), id, EstadoCita.CONFIRMADA);
    }

    @GetMapping("/ventana")
    public Map<String, Integer> ventana() {
        return Map.of("ventanaSemanas", citaService.ventanaSemanas());
    }
}
