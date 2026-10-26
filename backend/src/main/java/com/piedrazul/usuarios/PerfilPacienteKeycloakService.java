package com.piedrazul.usuarios;

import com.piedrazul.config.KeycloakJwtAuthenticationConverter;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.pacientes.PacienteRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Service
@Profile("keycloak")
public class PerfilPacienteKeycloakService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilPacienteKeycloakService(UsuarioRepository usuarioRepository,
                                         PacienteRepository pacienteRepository,
                                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioDto crearPerfil(PerfilPacienteKeycloakRequest request) {
        Jwt jwt = jwtActual();
        var roles = KeycloakJwtAuthenticationConverter.rolesAplicacion(jwt);
        if (roles.size() != 1 || roles.get(0) != Rol.PACIENTE) {
            throw new AccessDeniedException("Solo una cuenta Keycloak con el rol PACIENTE puede crear este perfil.");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (subject == null || subject.isBlank() || email == null || email.isBlank()
                || !Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new AccessDeniedException("La cuenta Keycloak debe tener identificador y correo verificado.");
        }

        String emailNormalizado = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByKeycloakSubject(subject).orElse(null);
        if (usuario == null) {
            usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado).orElse(null);
            if (usuario != null && usuario.getKeycloakSubject() != null
                    && !usuario.getKeycloakSubject().equals(subject)) {
                throw new AccessDeniedException("El correo ya está vinculado a otra cuenta Keycloak.");
            }
        }

        if (usuario != null && usuario.getRol() != Rol.PACIENTE) {
            throw new AccessDeniedException("El usuario local existente no tiene perfil de paciente.");
        }
        if (usuario != null && !usuario.isActivo()) {
            throw new AccessDeniedException("La cuenta local está deshabilitada.");
        }
        if (usuario != null) {
            Paciente perfilExistente = pacienteRepository.findByUsuario(usuario).orElse(null);
            if (perfilExistente != null) {
                if (!perfilExistente.getDocumento().equals(request.documento())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "La cuenta ya tiene un perfil de paciente con otro documento.");
                }
                usuario.setKeycloakSubject(subject);
                usuarioRepository.save(usuario);
                return dto(usuario, perfilExistente);
            }
        }
        if (pacienteRepository.existsByDocumento(request.documento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un paciente con ese documento.");
        }

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setEmail(emailNormalizado);
            usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setRol(Rol.PACIENTE);
            usuario.setActivo(true);
        }
        usuario.setKeycloakSubject(subject);
        String nombreDelToken = jwt.getClaimAsString("name");
        usuario.setNombreCompleto(nombreDelToken == null || nombreDelToken.isBlank()
                ? request.nombreCompleto().trim() : nombreDelToken.trim());
        usuario = usuarioRepository.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        paciente.setDocumento(request.documento());
        paciente.setTelefono(request.telefono().trim());
        paciente.setCodigo("PAC-" + String.format("%04d", usuario.getId()));
        paciente = pacienteRepository.save(paciente);
        return dto(usuario, paciente);
    }

    private Jwt jwtActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new AccessDeniedException("Se requiere un token JWT de Keycloak.");
    }

    private UsuarioDto dto(Usuario usuario, Paciente paciente) {
        UsuarioDto dto = UsuarioDto.from(usuario);
        dto.setPacienteId(paciente.getId());
        dto.setDocumento(paciente.getDocumento());
        dto.setTelefono(paciente.getTelefono());
        dto.setCodigoPaciente(paciente.getCodigo());
        return dto;
    }
}
