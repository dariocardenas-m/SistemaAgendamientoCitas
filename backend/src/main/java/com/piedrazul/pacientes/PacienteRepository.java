package com.piedrazul.pacientes;

import com.piedrazul.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByUsuario(Usuario usuario);

    Optional<Paciente> findByUsuarioId(Long usuarioId);

    boolean existsByDocumento(String documento);
}
