package com.piedrazul.pacientes;

import com.piedrazul.config.RecursoNoEncontradoException;
import com.piedrazul.config.SeguridadUtil;
import com.piedrazul.usuarios.Usuario;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteRepository pacienteRepository;
    private final SeguridadUtil seguridadUtil;

    public PacienteController(PacienteRepository pacienteRepository, SeguridadUtil seguridadUtil) {
        this.pacienteRepository = pacienteRepository;
        this.seguridadUtil = seguridadUtil;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public PacienteDto miPerfil() {
        Usuario usuario = seguridadUtil.usuarioActual();
        Paciente paciente = pacienteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de paciente no encontrado"));
        return PacienteDto.from(paciente);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR','MEDICO_TERAPISTA')")
    public PacienteDto porId(@PathVariable Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
        return PacienteDto.from(paciente);
    }
}
