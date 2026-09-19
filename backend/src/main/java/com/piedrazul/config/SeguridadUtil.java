package com.piedrazul.config;

import com.piedrazul.usuarios.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SeguridadUtil {

    public Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new org.springframework.security.access.AccessDeniedException("Sesión no válida");
        }
        return usuario;
    }
}
