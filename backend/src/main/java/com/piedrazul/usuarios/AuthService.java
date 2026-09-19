package com.piedrazul.usuarios;

import com.piedrazul.config.JwtService;
import com.piedrazul.config.ReglaNegocioException;
import com.piedrazul.config.SeguridadUtil;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.pacientes.PacienteRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SeguridadUtil seguridadUtil;

    public AuthService(UsuarioRepository usuarioRepository,
                       PacienteRepository pacienteRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SeguridadUtil seguridadUtil) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.seguridadUtil = seguridadUtil;
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("invalid"));
        if (!usuario.isActivo() || !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("invalid");
        }
        return emitir(usuario);
    }

    @Transactional
    public AuthResponse registrarPaciente(RegistroPacienteRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo electrónico.");
        }
        if (pacienteRepository.existsByDocumento(request.getDocumento())) {
            throw new ReglaNegocioException("Ya existe un paciente con ese documento.");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setEmail(request.getEmail().trim().toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(Rol.PACIENTE);
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        paciente.setDocumento(request.getDocumento());
        paciente.setTelefono(request.getTelefono());
        paciente.setCodigo("PAC-" + String.format("%04d", usuario.getId()));
        pacienteRepository.save(paciente);
        return emitir(usuario);
    }

    public UsuarioDto me() {
        return toDto(seguridadUtil.usuarioActual());
    }

    private AuthResponse emitir(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
        return new AuthResponse(token, toDto(usuario));
    }

    private UsuarioDto toDto(Usuario usuario) {
        UsuarioDto dto = UsuarioDto.from(usuario);
        pacienteRepository.findByUsuario(usuario).ifPresent(p -> {
            dto.setPacienteId(p.getId());
            dto.setDocumento(p.getDocumento());
            dto.setTelefono(p.getTelefono());
            dto.setCodigoPaciente(p.getCodigo());
        });
        return dto;
    }
}
