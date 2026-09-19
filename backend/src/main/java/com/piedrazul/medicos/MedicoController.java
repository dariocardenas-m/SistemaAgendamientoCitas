package com.piedrazul.medicos;

import com.piedrazul.config.RecursoNoEncontradoException;
import com.piedrazul.config.ReglaNegocioException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/medicos")
public class MedicoController {

    private static final Set<Integer> INTERVALOS = Set.of(15, 30, 45, 60);
    private static final Set<String> DIAS = Set.of("LUN", "MAR", "MIE", "JUE", "VIE", "SAB", "DOM");

    private final MedicoRepository medicoRepository;

    public MedicoController(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    @GetMapping
    public List<MedicoDto> listar() {
        return medicoRepository.findAllByOrderByNombreCompletoAsc().stream()
                .map(MedicoDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MedicoDto porId(@PathVariable Long id) {
        return MedicoDto.from(medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado")));
    }

    @PutMapping("/{id}/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MedicoDto actualizarConfig(@PathVariable Long id, @RequestBody MedicoConfigRequest request) {
        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        if (request.getIntervaloMinutos() == null || !INTERVALOS.contains(request.getIntervaloMinutos())) {
            throw new ReglaNegocioException("El intervalo debe ser 15, 30, 45 o 60 minutos.");
        }
        if (request.getDiasAtencion() == null || request.getDiasAtencion().isEmpty()) {
            throw new ReglaNegocioException("Debe configurar al menos un día de atención.");
        }
        for (String dia : request.getDiasAtencion()) {
            if (!DIAS.contains(dia)) {
                throw new ReglaNegocioException("Día de atención no válido: " + dia);
            }
        }
        if (request.getHoraApertura() == null || request.getHoraCierre() == null
                || !request.getHoraCierre().isAfter(request.getHoraApertura())) {
            throw new ReglaNegocioException("La hora de cierre debe ser posterior a la de apertura.");
        }
        medico.setDiasAtencion(String.join(",", request.getDiasAtencion()));
        medico.setHoraApertura(request.getHoraApertura());
        medico.setHoraCierre(request.getHoraCierre());
        medico.setIntervaloMinutos(request.getIntervaloMinutos());
        return MedicoDto.from(medicoRepository.save(medico));
    }
}
