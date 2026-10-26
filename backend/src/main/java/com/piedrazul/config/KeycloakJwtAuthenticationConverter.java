package com.piedrazul.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import com.piedrazul.usuarios.Rol;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = rolesAplicacion(jwt).stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name()))
                .map(GrantedAuthority.class::cast)
                .toList();
        String principalName = jwt.getClaimAsString("email");
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getClaimAsString("preferred_username");
        }
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    public static List<Rol> rolesAplicacion(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Object rolesClaim = realmAccess == null ? null : realmAccess.get("roles");
        List<?> roles = rolesClaim instanceof List<?> list ? list : List.of();
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::toUpperCase)
                .map(nombre -> {
                    try {
                        return Rol.valueOf(nombre);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}
