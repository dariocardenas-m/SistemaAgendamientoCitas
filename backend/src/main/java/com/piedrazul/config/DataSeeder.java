package com.piedrazul.config;

import com.piedrazul.especialidades.Especialidad;
import com.piedrazul.especialidades.EspecialidadRepository;
import com.piedrazul.medicos.Medico;
import com.piedrazul.medicos.MedicoRepository;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.pacientes.PacienteRepository;
import com.piedrazul.usuarios.Rol;
import com.piedrazul.usuarios.Usuario;
import com.piedrazul.usuarios.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final ConfiguracionGlobalRepository configuracionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      PacienteRepository pacienteRepository,
                      EspecialidadRepository especialidadRepository,
                      MedicoRepository medicoRepository,
                      ConfiguracionGlobalRepository configuracionRepository,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.configuracionRepository = configuracionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        ConfiguracionGlobal cfg = new ConfiguracionGlobal();
        cfg.setVentanaSemanas(8);
        configuracionRepository.save(cfg);

        usuario("Dra. Sofía Alarcón", "admin@piedrazul.com", passwordEncoder.encode("admin123"), Rol.ADMINISTRADOR);
        usuario("Carlos Mendoza", "carlos.mendoza@piedrazul.com", passwordEncoder.encode("agendador123"), Rol.AGENDADOR);
        Usuario juan = usuario("Juan Pérez", "juan.perez@email.com", passwordEncoder.encode("paciente123"), Rol.PACIENTE);

        paciente(juan, "1061789234", "312 456 7890", "PAC-0001");

        Especialidad gen = especialidad("Medicina General", "Atención primaria, chequeos preventivos y diagnóstico general", "heart", "#00A887");
        Especialidad ped = especialidad("Pediatría", "Cuidado especializado para bebés, niños y adolescentes", "baby", "#3B82F6");
        Especialidad fisio = especialidad("Fisioterapia", "Rehabilitación física, movilidad y recuperación muscular", "activity", "#F59E0B");
        Especialidad psico = especialidad("Psicología", "Salud mental, bienestar emocional y orientación profesional", "brain", "#A855F7");
        Especialidad odonto = especialidad("Odontología", "Salud bucal, limpieza, prevención y estética dental", "smile", "#14B8A6");

        medico("Dra. Laura Martínez", gen,
                "Consultorio 201 - Bloque A", 9, 4.9,
                "Especialista en atención médica integral preventiva, diagnóstico clínico y manejo de enfermedades crónicas.",
                "LUN,MAR,MIE,JUE,VIE", LocalTime.of(8, 0), LocalTime.of(12, 0), 30,
                "/avatars/laura.png");
        medico("Dr. Carlos Gómez", gen,
                "Consultorio 202 - Bloque A", 12, 4.8,
                "Médico cirujano con amplia trayectoria en medicina familiar, consulta externa y chequeos preventivos.",
                "LUN,MAR,MIE,JUE,VIE", LocalTime.of(8, 0), LocalTime.of(13, 0), 30,
                "/avatars/carlos.png");
        medico("Dra. Ana Rodríguez", ped,
                "Consultorio 104 - Área Infantil", 11, 4.8,
                "Pediatra con enfoque en crecimiento, vacunación y control del niño sano.",
                "LUN,MAR,MIE,JUE,VIE", LocalTime.of(8, 0), LocalTime.of(16, 0), 30,
                "/avatars/ana.png");
        medico("Lic. María Torres", fisio,
                "Sala de Fisioterapia - Piso 1", 5, 4.7,
                "Fisioterapeuta enfocada en rehabilitación musculoesquelética y terapia funcional.",
                "LUN,MAR,MIE,JUE,VIE", LocalTime.of(7, 0), LocalTime.of(15, 0), 45,
                "/avatars/maria.png");
        medico("Psic. Andrés Molina", psico,
                "Consultorio 310 - Salud Mental", 8, 4.9,
                "Psicólogo clínico con experiencia en terapia cognitivo-conductual y orientación familiar.",
                "LUN,MAR,MIE,JUE", LocalTime.of(9, 0), LocalTime.of(17, 0), 45,
                "/avatars/andres.png");
        medico("Dra. Lucía Herrera", odonto,
                "Consultorio Odontología - Bloque B", 7, 4.6,
                "Odontóloga general con énfasis en prevención, endodoncia básica y estética dental.",
                "MAR,MIE,JUE,VIE,SAB", LocalTime.of(8, 0), LocalTime.of(14, 0), 30,
                "/avatars/lucia.png");

    }

    private Usuario usuario(String nombre, String email, String hash, Rol rol) {
        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setRol(rol);
        u.setActivo(true);
        return usuarioRepository.save(u);
    }

    private Paciente paciente(Usuario u, String doc, String tel, String codigo) {
        Paciente p = new Paciente();
        p.setUsuario(u);
        p.setDocumento(doc);
        p.setTelefono(tel);
        p.setCodigo(codigo);
        return pacienteRepository.save(p);
    }

    private Especialidad especialidad(String nombre, String desc, String icono, String color) {
        Especialidad e = new Especialidad();
        e.setNombre(nombre);
        e.setDescripcion(desc);
        e.setIcono(icono);
        e.setColor(color);
        return especialidadRepository.save(e);
    }

    private Medico medico(String nombre, Especialidad esp, String consultorio, int anios, double rating,
                          String desc, String dias, LocalTime apertura, LocalTime cierre, int intervalo, String foto) {
        Medico m = new Medico();
        m.setNombreCompleto(nombre);
        m.setEspecialidad(esp);
        m.setConsultorio(consultorio);
        m.setAniosExperiencia(anios);
        m.setRating(rating);
        m.setDescripcion(desc);
        m.setDiasAtencion(dias);
        m.setHoraApertura(apertura);
        m.setHoraCierre(cierre);
        m.setIntervaloMinutos(intervalo);
        m.setFotoUrl(foto);
        return medicoRepository.save(m);
    }

}
