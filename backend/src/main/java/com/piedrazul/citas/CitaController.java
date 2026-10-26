package com.piedrazul.citas;

import com.piedrazul.config.SeguridadUtil;
import com.piedrazul.usuarios.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@Validated
public class CitaController {

    private final CitaService citaService;
    private final SeguridadUtil seguridadUtil;
    private final CitaCsvExportService citaCsvExportService;

    public CitaController(CitaService citaService, SeguridadUtil seguridadUtil, CitaCsvExportService citaCsvExportService) {
        this.citaService = citaService;
        this.seguridadUtil = seguridadUtil;
        this.citaCsvExportService = citaCsvExportService;
    }

    @GetMapping("/fechas-habiles")
    public List<LocalDate> fechasHabiles(@RequestParam Long medicoId) {
        return citaService.fechasHabiles(medicoId);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadDto disponibilidad(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long excluirCitaId) {
        return citaService.disponibilidad(medicoId, fecha, excluirCitaId);
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
        return citaService.consultarAgenda(seguridadUtil.usuarioActual(), medicoId, fecha);
    }

    @GetMapping(value = "/exportar", produces = "text/csv")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR','MEDICO_TERAPISTA')")
    public ResponseEntity<byte[]> exportar(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        byte[] csv = citaCsvExportService.generar(
                citaService.exportarAgenda(seguridadUtil.usuarioActual(), medicoId, fecha));
        String nombre = "agenda-" + fecha + "-medico-" + medicoId + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                .body(csv);
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR','MEDICO_TERAPISTA')")
    public List<HistorialCitaDto> historial(
            @RequestParam @Pattern(regexp = "\\d{6,20}", message = "La cédula debe contener entre 6 y 20 dígitos.") String cedula) {
        return citaService.historialPorDocumento(seguridadUtil.usuarioActual(), cedula);
    }

    @GetMapping("/{id}")
    public CitaDto porId(@PathVariable Long id) {
        return citaService.porId(seguridadUtil.usuarioActual(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE','AGENDADOR','ADMINISTRADOR')")
    public CitaDto editar(@PathVariable Long id, @Valid @RequestBody EditarCitaRequest request) {
        return citaService.editar(seguridadUtil.usuarioActual(), id, request);
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
