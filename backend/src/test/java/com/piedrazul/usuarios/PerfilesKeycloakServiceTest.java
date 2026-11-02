package com.piedrazul.usuarios;

import com.piedrazul.pacientes.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilesKeycloakServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock PacienteRepository pacientes;
    @Mock PasswordEncoder encoder;
    private PerfilBasicoKeycloakService basico;
    private PerfilPacienteKeycloakService paciente;
    @BeforeEach void init() {basico=new PerfilBasicoKeycloakService(usuarios,encoder);paciente=new PerfilPacienteKeycloakService(usuarios,pacientes,encoder);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    private void identidad(Rol rol,boolean verified) {
        Jwt j=Jwt.withTokenValue("token-ficticio").header("alg","RS256").subject("subject-prueba")
            .claim("email","persona@example.test").claim("email_verified",verified)
            .claim("realm_access",Map.of("roles",List.of(rol.name()))).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(j));
    }
    private Usuario usuario(Rol rol){Usuario u=new Usuario();u.setId(9L);u.setRol(rol);u.setEmail("persona@example.test");u.setKeycloakSubject("subject-prueba");return u;}
    private PerfilPacienteKeycloakRequest request(){return new PerfilPacienteKeycloakRequest(" Persona ","12345678"," 3000000000 ");}
    @Test void perfilBasicoRechazaSesionSinJwt(){assertThrows(AccessDeniedException.class,()->basico.crearPerfil(new PerfilBasicoKeycloakRequest("Persona")));verifyNoInteractions(usuarios);}
    @Test void perfilBasicoRechazaRolPaciente(){identidad(Rol.PACIENTE,true);assertThrows(AccessDeniedException.class,()->basico.crearPerfil(new PerfilBasicoKeycloakRequest("Persona")));}
    @Test void perfilBasicoRechazaCorreoNoVerificado(){identidad(Rol.AGENDADOR,false);assertThrows(AccessDeniedException.class,()->basico.crearPerfil(new PerfilBasicoKeycloakRequest("Persona")));verifyNoInteractions(usuarios);}
    @Test void perfilBasicoCreaIdentidadVinculadaConContrasenaNoReutilizableDelToken(){
        identidad(Rol.AGENDADOR,true);when(encoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(usuarios.save(any())).thenAnswer(inv->{Usuario u=inv.getArgument(0);u.setId(9L);return u;});
        UsuarioDto dto=basico.crearPerfil(new PerfilBasicoKeycloakRequest(" Persona "));
        assertEquals(9L,dto.getId());assertEquals(Rol.AGENDADOR,dto.getRol());assertEquals("Persona",dto.getNombreCompleto());
        ArgumentCaptor<Usuario> c=ArgumentCaptor.forClass(Usuario.class);verify(usuarios).save(c.capture());
        assertEquals("subject-prueba",c.getValue().getKeycloakSubject());assertEquals("hash-aleatorio",c.getValue().getPasswordHash());
    }
    @Test void perfilBasicoNoReactivaCuentaDeshabilitada(){
        identidad(Rol.ADMINISTRADOR,true);Usuario u=usuario(Rol.ADMINISTRADOR);u.setActivo(false);
        when(usuarios.findByKeycloakSubject("subject-prueba")).thenReturn(Optional.of(u));
        assertThrows(AccessDeniedException.class,()->basico.crearPerfil(new PerfilBasicoKeycloakRequest("Persona")));verify(usuarios,never()).save(any());
    }
    @Test void perfilPacienteRechazaRolAdministrativo(){identidad(Rol.ADMINISTRADOR,true);assertThrows(AccessDeniedException.class,()->paciente.crearPerfil(request()));verifyNoInteractions(pacientes);}
    @Test void perfilPacienteRechazaCorreoNoVerificado(){identidad(Rol.PACIENTE,false);assertThrows(AccessDeniedException.class,()->paciente.crearPerfil(request()));}
    @Test void perfilPacienteNoUsurpaCorreoYaVinculado(){
        identidad(Rol.PACIENTE,true);Usuario u=usuario(Rol.PACIENTE);u.setKeycloakSubject("otra-identidad");
        when(usuarios.findByEmailIgnoreCase("persona@example.test")).thenReturn(Optional.of(u));
        assertThrows(AccessDeniedException.class,()->paciente.crearPerfil(request()));verify(pacientes,never()).save(any());
    }
    @Test void perfilPacienteRechazaDocumentoOcupado(){identidad(Rol.PACIENTE,true);when(pacientes.existsByDocumento("12345678")).thenReturn(true);assertThrows(ResponseStatusException.class,()->paciente.crearPerfil(request()));verify(usuarios,never()).save(any());}
    @Test void perfilPacienteEsIdempotenteParaLaMismaCuentaYDocumento(){
        identidad(Rol.PACIENTE,true);Usuario u=usuario(Rol.PACIENTE);Paciente p=new Paciente();p.setId(2L);p.setUsuario(u);p.setDocumento("12345678");p.setCodigo("PAC-0009");
        when(usuarios.findByKeycloakSubject("subject-prueba")).thenReturn(Optional.of(u));when(pacientes.findByUsuario(u)).thenReturn(Optional.of(p));
        assertEquals(2L,paciente.crearPerfil(request()).getPacienteId());verify(pacientes,never()).save(any());verifyNoInteractions(encoder);
    }
    @Test void perfilPacienteNoReasignaDocumentoDePerfilExistente(){
        identidad(Rol.PACIENTE,true);Usuario u=usuario(Rol.PACIENTE);Paciente p=new Paciente();p.setDocumento("87654321");
        when(usuarios.findByKeycloakSubject("subject-prueba")).thenReturn(Optional.of(u));when(pacientes.findByUsuario(u)).thenReturn(Optional.of(p));
        assertThrows(ResponseStatusException.class,()->paciente.crearPerfil(request()));verify(pacientes,never()).save(any());
    }
    @Test void perfilPacienteCreaCuentaYPerfilConDocumentoNormalizado(){
        identidad(Rol.PACIENTE,true);when(encoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(usuarios.save(any())).thenAnswer(inv->{Usuario u=inv.getArgument(0);u.setId(9L);return u;});
        when(pacientes.save(any())).thenAnswer(inv->{Paciente p=inv.getArgument(0);p.setId(2L);return p;});
        UsuarioDto dto=paciente.crearPerfil(request());assertEquals(2L,dto.getPacienteId());assertEquals("12345678",dto.getDocumento());assertEquals("3000000000",dto.getTelefono());assertEquals("PAC-0009",dto.getCodigoPaciente());
    }
}
