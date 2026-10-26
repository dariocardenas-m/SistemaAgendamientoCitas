package com.piedrazul.usuarios;

import com.piedrazul.config.KeycloakJwtAuthenticationConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Profile("keycloak")
public class PerfilBasicoKeycloakService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilBasicoKeycloakService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioDto crearPerfil(PerfilBasicoKeycloakRequest request) {
        Jwt jwt = jwtActual();
        var roles = KeycloakJwtAuthenticationConverter.rolesAplicacion(jwt);
        if (roles.size() != 1 || (roles.get(0) != Rol.ADMINISTRADOR && roles.get(0) != Rol.AGENDADOR)) {
            throw new AccessDeniedException("Este perfil solo admite cuentas con rol ADMINISTRADOR o AGENDADOR en Keycloak.");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (subject == null || subject.isBlank() || email == null || email.isBlank()
                || !Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new AccessDeniedException("La cuenta Keycloak debe tener identificador y correo verificado.");
        }

        Usuario usuario = usuarioRepository.findByKeycloakSubject(subject).orElse(null);
        String emailNormalizado = email.trim().toLowerCase();
        if (usuario == null) {
            usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado).orElse(null);
            if (usuario != null && usuario.getKeycloakSubject() != null
                    && !usuario.getKeycloakSubject().equals(subject)) {
                throw new AccessDeniedException("El correo ya está vinculado a otra cuenta Keycloak.");
            }
        }

        Usuario usuarioConEseCorreo = usuarioRepository.findByEmailIgnoreCase(emailNormalizado).orElse(null);
        if (usuarioConEseCorreo != null && usuario != null
                && !usuarioConEseCorreo.getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya pertenece a otro perfil local.");
        }

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setEmail(emailNormalizado);
            usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setActivo(true);
        } else if (!usuario.isActivo()) {
            throw new AccessDeniedException("La cuenta local está deshabilitada.");
        }

        usuario.setEmail(emailNormalizado);
        usuario.setKeycloakSubject(subject);
        usuario.setRol(roles.get(0));
        String nombreDelToken = jwt.getClaimAsString("name");
        usuario.setNombreCompleto(nombreDelToken == null || nombreDelToken.isBlank()
                ? request.nombreCompleto().trim() : nombreDelToken.trim());
        return UsuarioDto.from(usuarioRepository.save(usuario));
    }

    private Jwt jwtActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new AccessDeniedException("Se requiere un token JWT de Keycloak.");
    }
}
