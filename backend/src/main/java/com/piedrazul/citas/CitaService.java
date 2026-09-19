package com.piedrazul.citas;

import com.piedrazul.config.ConflictoReservaException;
import com.piedrazul.config.ConfiguracionGlobal;
import com.piedrazul.config.ConfiguracionGlobalRepository;
import com.piedrazul.config.RecursoNoEncontradoException;
import com.piedrazul.config.ReglaNegocioException;
import com.piedrazul.medicos.Medico;
import com.piedrazul.medicos.MedicoRepository;
import com.piedrazul.pacientes.Paciente;
import com.piedrazul.pacientes.PacienteRepository;
import com.piedrazul.usuarios.Rol;
import com.piedrazul.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final ConfiguracionGlobalRepository configuracionRepository;

    public CitaService(CitaRepository citaRepository,
                       MedicoRepository medicoRepository,
                       PacienteRepository pacienteRepository,
                       ConfiguracionGlobalRepository configuracionRepository) {
        this.citaRepository = citaRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.configuracionRepository = configuracionRepository;
    }

    public int ventanaSemanas() {
        return configuracionRepository.findAll().stream()
                .findFirst()
                .map(ConfiguracionGlobal::getVentanaSemanas)
                .orElse(4);
    }

    public List<LocalDate> fechasHabiles(Long medicoId) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        Set<DayOfWeek> dias = parseDias(medico.getDiasAtencion());
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusWeeks(ventanaSemanas());
        List<LocalDate> fechas = new ArrayList<>();
        for (LocalDate d = hoy; !d.isAfter(limite); d = d.plusDays(1)) {
            if (dias.contains(d.getDayOfWeek())) {
                fechas.add(d);
            }
        }
        return fechas;
    }

    public DisponibilidadDto disponibilidad(Long medicoId, LocalDate fecha) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        validarFechaHabil(medico, fecha);
        List<LocalTime> todas = generarFranjas(medico);
        List<Cita> delDia = citaRepository.findByMedicoIdAndFechaOrderByHoraAsc(medicoId, fecha);
        List<LocalTime> ocupadas = delDia.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .map(Cita::getHora)
                .toList();
        List<LocalTime> libres = todas.stream().filter(h -> !ocupadas.contains(h)).toList();
        if (fecha.equals(LocalDate.now())) {
            LocalTime ahora = LocalTime.now();
            libres = libres.stream().filter(h -> h.isAfter(ahora)).toList();
        }
        return new DisponibilidadDto(libres, ocupadas, medico.getIntervaloMinutos());
    }

    @Transactional
    public CitaDto reservar(Usuario actor, ReservarCitaRequest request) {
        Paciente paciente = resolverPaciente(actor, request.getPacienteId());
        Medico medico = medicoRepository.lockById(request.getMedicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        validarFechaHabil(medico, request.getFecha());
        List<LocalTime> franjas = generarFranjas(medico);
        if (!franjas.contains(request.getHora())) {
            throw new ReglaNegocioException("La hora seleccionada no corresponde a una franja válida del profesional.");
        }
        // El bloqueo del médico serializa reservas aun cuando todavía no exista una cita ese día.
        // La consulta bloqueada protege además las citas existentes mientras se valida el rango.
        List<Cita> citasBloqueadas = citaRepository.lockByMedicoAndFecha(medico.getId(), request.getFecha());
        if (haySolapamiento(citasBloqueadas, request.getHora(), medico.getIntervaloMinutos())) {
            throw new ConflictoReservaException(
                    "La franja horaria seleccionada acaba de ser ocupada por otro usuario. Seleccione otro horario.");
        }
        Cita cita = new Cita();
        cita.setCodigo(generarCodigo(request.getFecha()));
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFecha(request.getFecha());
        cita.setHora(request.getHora());
        cita.setDuracionMinutos(medico.getIntervaloMinutos());
        cita.setModalidad(request.getModalidad() == null ? ModalidadCita.PRESENCIAL : request.getModalidad());
        cita.setMotivo(request.getMotivo());
        cita.setEstado(EstadoCita.CONFIRMADA);
        return CitaDto.from(citaRepository.save(cita));
    }

    public List<CitaDto> citasPaciente(Usuario actor) {
        Paciente paciente = pacienteRepository.findByUsuario(actor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de paciente no encontrado"));
        return citaRepository.findByPacienteIdOrderByFechaDescHoraDesc(paciente.getId())
                .stream().map(CitaDto::from).toList();
    }

    public List<CitaDto> consultarAgenda(Long medicoId, LocalDate fecha) {
        if (!medicoRepository.existsById(medicoId)) {
            throw new RecursoNoEncontradoException("Médico no encontrado");
        }
        return citaRepository.findByMedicoIdAndFechaOrderByHoraAsc(medicoId, fecha)
                .stream().map(CitaDto::from).toList();
    }

    @Transactional
    public CitaDto cambiarEstado(Usuario actor, Long citaId, EstadoCita nuevo) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        if (actor.getRol() == Rol.PACIENTE) {
            Paciente p = pacienteRepository.findByUsuario(actor)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
            if (!cita.getPaciente().getId().equals(p.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("No puede modificar esta cita");
            }
            if (nuevo != EstadoCita.CANCELADA) {
                throw new ReglaNegocioException("El paciente solo puede cancelar sus citas.");
            }
        }
        cita.setEstado(nuevo);
        return CitaDto.from(citaRepository.save(cita));
    }

    public CitaDto porId(Long id) {
        return CitaDto.from(citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada")));
    }

    private Paciente resolverPaciente(Usuario actor, Long pacienteId) {
        if (actor.getRol() == Rol.PACIENTE) {
            return pacienteRepository.findByUsuario(actor)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de paciente no encontrado"));
        }
        if (pacienteId == null) {
            throw new ReglaNegocioException("Debe indicar el paciente para agendar.");
        }
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
    }

    private void validarFechaHabil(Medico medico, LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusWeeks(ventanaSemanas()))) {
            throw new ReglaNegocioException("La fecha está fuera de la ventana de agendamiento configurada.");
        }
        Set<DayOfWeek> dias = parseDias(medico.getDiasAtencion());
        if (!dias.contains(fecha.getDayOfWeek())) {
            throw new ReglaNegocioException("El profesional no atiende en la fecha seleccionada.");
        }
    }

    private List<LocalTime> generarFranjas(Medico medico) {
        List<LocalTime> franjas = new ArrayList<>();
        LocalTime t = medico.getHoraApertura();
        LocalTime cierre = medico.getHoraCierre();
        int intervalo = medico.getIntervaloMinutos();
        while (!t.plusMinutes(intervalo).isAfter(cierre) && t.isBefore(cierre)) {
            franjas.add(t);
            t = t.plusMinutes(intervalo);
        }
        return franjas;
    }

    private boolean haySolapamiento(List<Cita> citas, LocalTime horaSolicitada, int duracionSolicitada) {
        LocalTime finSolicitado = horaSolicitada.plusMinutes(duracionSolicitada);
        return citas.stream()
                .filter(cita -> cita.getEstado() != EstadoCita.CANCELADA)
                .anyMatch(cita -> {
                    LocalTime finExistente = cita.getHora().plusMinutes(cita.getDuracionMinutos());
                    return cita.getHora().isBefore(finSolicitado) && finExistente.isAfter(horaSolicitada);
                });
    }

    private Set<DayOfWeek> parseDias(String csv) {
        EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (String d : csv.split(",")) {
            set.add(switch (d.trim()) {
                case "LUN" -> DayOfWeek.MONDAY;
                case "MAR" -> DayOfWeek.TUESDAY;
                case "MIE" -> DayOfWeek.WEDNESDAY;
                case "JUE" -> DayOfWeek.THURSDAY;
                case "VIE" -> DayOfWeek.FRIDAY;
                case "SAB" -> DayOfWeek.SATURDAY;
                case "DOM" -> DayOfWeek.SUNDAY;
                default -> throw new ReglaNegocioException("Día inválido: " + d);
            });
        }
        return set;
    }

    private String generarCodigo(LocalDate fecha) {
        String codigo;
        do {
            int n = ThreadLocalRandom.current().nextInt(1000, 10000);
            codigo = "CITA-" + fecha.getYear() + "-" + n;
        } while (citaRepository.existsByCodigo(codigo));
        return codigo;
    }
}
