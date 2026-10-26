package com.piedrazul.config;

import com.piedrazul.usuarios.Usuario;
import com.piedrazul.usuarios.UsuarioRepository;
import com.piedrazul.usuarios.Rol;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SeguridadUtil {

    private final UsuarioRepository usuarioRepository;

    public SeguridadUtil(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            List<Rol> roles = KeycloakJwtAuthenticationConverter.rolesAplicacion(jwt);
            if (subject == null || subject.isBlank() || roles.size() != 1) {
                throw new AccessDeniedException("El token debe tener un identificador y exactamente un rol del sistema.");
            }

            Usuario usuario = usuarioRepository.findByKeycloakSubject(subject).orElse(null);
            if (usuario == null) {
                String email = jwt.getClaimAsString("email");
                Boolean emailVerificado = jwt.getClaim("email_verified");
                if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerificado)) {
                    throw new AccessDeniedException("El token debe incluir un correo verificado para vincular el perfil.");
                }
                usuario = usuarioRepository.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new AccessDeniedException("No existe un perfil local vinculado a esta cuenta."));
                if (usuario.getKeycloakSubject() != null && !usuario.getKeycloakSubject().equals(subject)) {
                    throw new AccessDeniedException("El perfil local ya está vinculado a otra cuenta de identidad.");
                }
                usuario.setKeycloakSubject(subject);
                usuarioRepository.save(usuario);
            }

            if (!usuario.isActivo()) {
                throw new AccessDeniedException("Usuario no habilitado en el sistema.");
            }
            return usuarioConRol(usuario, roles.get(0));
        }
        throw new AccessDeniedException("Sesión no válida");
    }

    private Usuario usuarioConRol(Usuario original, Rol rolDelToken) {
        Usuario actor = new Usuario();
        actor.setId(original.getId());
        actor.setNombreCompleto(original.getNombreCompleto());
        actor.setEmail(original.getEmail());
        actor.setRol(rolDelToken);
        actor.setActivo(original.isActivo());
        actor.setFotoUrl(original.getFotoUrl());
        actor.setKeycloakSubject(original.getKeycloakSubject());
        return actor;
    }
}
