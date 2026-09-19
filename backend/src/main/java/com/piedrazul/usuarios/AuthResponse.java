package com.piedrazul.usuarios;

public class AuthResponse {

    private String token;
    private UsuarioDto usuario;

    public AuthResponse(String token, UsuarioDto usuario) {
        this.token = token;
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public UsuarioDto getUsuario() {
        return usuario;
    }
}
