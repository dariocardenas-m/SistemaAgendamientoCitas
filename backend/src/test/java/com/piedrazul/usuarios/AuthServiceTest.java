package com.piedrazul.usuarios;

import com.piedrazul.config.*;
import com.piedrazul.pacientes.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock PacienteRepository pacientes;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock SeguridadUtil seguridad;
    @InjectMocks AuthService service;

    private Usuario usuario() {
        Usuario u=new Usuario(); u.setId(8L); u.setEmail("persona@example.test");
        u.setNombreCompleto("Persona"); u.setPasswordHash("hash"); u.setRol(Rol.PACIENTE); return u;
    }
    private LoginRequest login() {
        LoginRequest r=new LoginRequest();r.setEmail("persona@example.test");r.setPassword("clave-ficticia");return r;
    }
    private RegistroPacienteRequest registro() {
        RegistroPacienteRequest r=new RegistroPacienteRequest();r.setNombreCompleto(" Persona ");
        r.setEmail(" Persona@Example.Test ");r.setDocumento("12345678");r.setTelefono("3000000000");r.setPassword("clave-ficticia");return r;
    }
    @Test void loginValidoEmiteTokenYPerfil() {
        Usuario u=usuario();when(usuarios.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        when(encoder.matches("clave-ficticia","hash")).thenReturn(true);
        when(jwt.generarToken(8L,u.getEmail(),Rol.PACIENTE)).thenReturn("token-prueba");
        AuthResponse res=service.login(login());
        assertEquals("token-prueba",res.getToken());assertEquals(8L,res.getUsuario().getId());
    }
    @Test void usuarioDesconocidoNoEmiteToken() {
        assertThrows(BadCredentialsException.class,()->service.login(login()));verifyNoInteractions(jwt);
    }
    @Test void contrasenaErroneaNoEmiteToken() {
        when(usuarios.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(usuario()));
        assertThrows(BadCredentialsException.class,()->service.login(login()));verifyNoInteractions(jwt);
    }
    @Test void usuarioInactivoNoValidaContrasenaNiEmiteToken() {
        Usuario u=usuario();u.setActivo(false);when(usuarios.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(u));
        assertThrows(BadCredentialsException.class,()->service.login(login()));verifyNoInteractions(encoder,jwt);
    }
    @Test void registroCifraContrasenaNormalizaYRelacionaPaciente() {
        when(encoder.encode("clave-ficticia")).thenReturn("hash-seguro");
        when(usuarios.save(any())).thenAnswer(inv->{Usuario u=inv.getArgument(0);u.setId(8L);return u;});
        service.registrarPaciente(registro());
        ArgumentCaptor<Usuario> uc=ArgumentCaptor.forClass(Usuario.class);verify(usuarios).save(uc.capture());
        assertEquals("persona@example.test",uc.getValue().getEmail());assertEquals("Persona",uc.getValue().getNombreCompleto());
        assertEquals("hash-seguro",uc.getValue().getPasswordHash());assertEquals(Rol.PACIENTE,uc.getValue().getRol());
        ArgumentCaptor<Paciente> pc=ArgumentCaptor.forClass(Paciente.class);verify(pacientes).save(pc.capture());
        assertSame(uc.getValue(),pc.getValue().getUsuario());assertEquals("PAC-0008",pc.getValue().getCodigo());
    }
    @Test void registroRechazaCorreoDuplicado() {
        when(usuarios.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        assertThrows(ReglaNegocioException.class,()->service.registrarPaciente(registro()));verify(usuarios,never()).save(any());
    }
    @Test void registroRechazaDocumentoDuplicado() {
        when(pacientes.existsByDocumento("12345678")).thenReturn(true);
        assertThrows(ReglaNegocioException.class,()->service.registrarPaciente(registro()));verifyNoInteractions(encoder,jwt);
    }
    @Test void meObtieneLaIdentidadDelContextoYNoUnaRecibidaDelCliente() {
        Usuario u=usuario();when(seguridad.usuarioActual()).thenReturn(u);
        assertEquals(u.getId(),service.me().getId());verify(seguridad).usuarioActual();
    }
}
