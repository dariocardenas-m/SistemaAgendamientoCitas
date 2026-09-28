package com.piedrazul.usuarios;
import com.piedrazul.Fixtures;
import com.piedrazul.config.*;
import com.piedrazul.pacientes.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UsuarioRepository usuarios; @Mock PacienteRepository pacientes;
    @Mock PasswordEncoder encoder; @Mock JwtService jwt; @Mock SeguridadUtil seguridad;
    @InjectMocks AuthService service;
    LoginRequest login() { var r=new LoginRequest(); r.setEmail("u1@example.test"); r.setPassword("password-prueba"); return r; }
    RegistroPacienteRequest registro() {
        var r=new RegistroPacienteRequest(); r.setEmail("NUEVO@example.test"); r.setNombreCompleto("  Paciente Prueba  ");
        r.setPassword("password-prueba"); r.setDocumento("1234567"); r.setTelefono("3000000000"); return r;
    }
    @Test void loginValidoEmiteTokenYPerfil() {
        Usuario u=Fixtures.usuario(1,Rol.PACIENTE); when(usuarios.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(u));
        when(encoder.matches("password-prueba",u.getPasswordHash())).thenReturn(true);
        when(jwt.generarToken(1L,u.getEmail(),Rol.PACIENTE)).thenReturn("token-prueba");
        assertThat(service.login(login()).getToken()).isEqualTo("token-prueba");
    }
    @Test void correoDesconocidoNoEmiteToken() {
        assertThatThrownBy(()->service.login(login())).isInstanceOf(BadCredentialsException.class); verifyNoInteractions(jwt);
    }
    @Test void claveIncorrectaNoEmiteToken() {
        when(usuarios.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(Fixtures.usuario(1,Rol.PACIENTE)));
        assertThatThrownBy(()->service.login(login())).isInstanceOf(BadCredentialsException.class); verifyNoInteractions(jwt);
    }
    @Test void usuarioInactivoNoAccede() {
        Usuario u=Fixtures.usuario(1,Rol.PACIENTE); u.setActivo(false);
        when(usuarios.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(u));
        assertThatThrownBy(()->service.login(login())).isInstanceOf(BadCredentialsException.class); verifyNoInteractions(jwt,encoder);
    }
    @Test void registroNormalizaCorreoAplicaHashYFuerzaRolPaciente() {
        when(encoder.encode("password-prueba")).thenReturn("hash-seguro");
        when(usuarios.save(any())).thenAnswer(i->{ Usuario u=i.getArgument(0); u.setId(42L); return u; });
        service.registrarPaciente(registro());
        ArgumentCaptor<Usuario> u=ArgumentCaptor.forClass(Usuario.class); verify(usuarios).save(u.capture());
        assertThat(u.getValue().getEmail()).isEqualTo("nuevo@example.test");
        assertThat(u.getValue().getNombreCompleto()).isEqualTo("Paciente Prueba");
        assertThat(u.getValue().getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(u.getValue().getRol()).isEqualTo(Rol.PACIENTE);
        ArgumentCaptor<Paciente> p=ArgumentCaptor.forClass(Paciente.class); verify(pacientes).save(p.capture());
        assertThat(p.getValue().getUsuario()).isSameAs(u.getValue()); assertThat(p.getValue().getCodigo()).isEqualTo("PAC-0042");
    }
    @Test void correoDuplicadoRechazadoSinPersistir() {
        when(usuarios.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        assertThatThrownBy(()->service.registrarPaciente(registro())).isInstanceOf(ReglaNegocioException.class);
        verify(usuarios,never()).save(any()); verify(pacientes,never()).save(any());
    }
    @Test void documentoDuplicadoRechazadoSinPersistir() {
        when(pacientes.existsByDocumento(anyString())).thenReturn(true);
        assertThatThrownBy(()->service.registrarPaciente(registro())).isInstanceOf(ReglaNegocioException.class);
        verify(usuarios,never()).save(any());
    }
    @Test void meRetornaPerfilAutenticado() {
        Paciente p=Fixtures.paciente(7); when(seguridad.usuarioActual()).thenReturn(p.getUsuario());
        when(pacientes.findByUsuario(p.getUsuario())).thenReturn(Optional.of(p));
        assertThat(service.me().getPacienteId()).isEqualTo(7L);
    }
}
