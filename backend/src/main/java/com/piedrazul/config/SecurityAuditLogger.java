package com.piedrazul.config;

import com.piedrazul.usuarios.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

@Component
public class SecurityAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void accesoDenegado(Usuario usuario, String operacion, Long medicoId) {
        LOGGER.warn("security_event=ACCESS_DENIED operation={} actorId={} role={} medicoId={}",
                operacion, usuario.getId(), usuario.getRol(), medicoId);
    }

    public void consultaExitosa(Usuario usuario, String operacion) {
        LOGGER.info("security_event=AUTHORIZED_ACCESS operation={} actorId={} role={}",
                operacion, usuario.getId(), usuario.getRol());
    }

    public void solicitudRechazada(Authentication authentication, String recurso) {
        String actor = authentication == null ? "anonymous" : authentication.getName();
        LOGGER.warn("security_event=ACCESS_DENIED resource={} actor={}", recurso, actor);
    }
}
