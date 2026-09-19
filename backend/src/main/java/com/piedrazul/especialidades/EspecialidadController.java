package com.piedrazul.especialidades;

import com.piedrazul.config.RecursoNoEncontradoException;
import com.piedrazul.medicos.MedicoDto;
import com.piedrazul.medicos.MedicoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;

    public EspecialidadController(EspecialidadRepository especialidadRepository, MedicoRepository medicoRepository) {
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
    }

    @GetMapping
    public List<EspecialidadDto> listar() {
        return especialidadRepository.findAll().stream()
                .map(e -> new EspecialidadDto(e, medicoRepository.countByEspecialidadId(e.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public EspecialidadDto porId(@PathVariable Long id) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));
        return new EspecialidadDto(e, medicoRepository.countByEspecialidadId(e.getId()));
    }

    @GetMapping("/{id}/medicos")
    public List<MedicoDto> medicos(@PathVariable Long id) {
        if (!especialidadRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Especialidad no encontrada");
        }
        return medicoRepository.findByEspecialidadIdOrderByNombreCompletoAsc(id).stream()
                .map(MedicoDto::from)
                .toList();
    }
}
