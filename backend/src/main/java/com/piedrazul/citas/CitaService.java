package com.piedrazul.citas;

import com.piedrazul.config.ConflictoReservaException;
import com.piedrazul.config.ConfiguracionGlobal;
import com.piedrazul.config.ConfiguracionGlobalRepository;
import com.piedrazul.config.RecursoNoEncontradoException;
import com.piedrazul.config.ReglaNegocioException;
import com.piedrazul.config.SecurityAuditLogger;
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
    private final SecurityAuditLogger securityAuditLogger;

    public CitaService(CitaRepository citaRepository,
                       MedicoRepository medicoRepository,
                       PacienteRepository pacienteRepository,
                       ConfiguracionGlobalRepository configuracionRepository,
                       SecurityAuditLogger securityAuditLogger) {
        this.citaRepository = citaRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.configuracionRepository = configuracionRepository;
        this.securityAuditLogger = securityAuditLogger;
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
        return disponibilidad(medicoId, fecha, null);
    }

    public DisponibilidadDto disponibilidad(Long medicoId, LocalDate fecha, Long excluirCitaId) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        validarFechaHabil(medico, fecha);
        List<LocalTime> todas = generarFranjas(medico);
        List<Cita> delDia = citaRepository.findByMedicoIdAndFechaOrderByHoraAsc(medicoId, fecha);
        List<LocalTime> ocupadas = delDia.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .filter(c -> excluirCitaId == null || !excluirCitaId.equals(c.getId()))
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

    public List<CitaDto> consultarAgenda(Usuario actor, Long medicoId, LocalDate fecha) {
        validarAccesoAgenda(actor, medicoId, "CONSULTAR_AGENDA");
        if (!medicoRepository.existsById(medicoId)) {
            throw new RecursoNoEncontradoException("Médico no encontrado");
        }
        return citaRepository.findByMedicoIdAndFechaOrderByHoraAsc(medicoId, fecha)
                .stream().map(CitaDto::from).toList();
    }

    public List<CitaDto> exportarAgenda(Usuario actor, Long medicoId, LocalDate fecha) {
        validarAccesoAgenda(actor, medicoId, "EXPORTAR_AGENDA_CSV");
        if (!medicoRepository.existsById(medicoId)) {
            throw new RecursoNoEncontradoException("Médico no encontrado");
        }
        return citaRepository.findByMedicoIdAndFechaOrderByHoraAsc(medicoId, fecha)
                .stream().map(CitaDto::from).toList();
    }

    public List<HistorialCitaDto> historialPorDocumento(Usuario actor, String documento) {
        Paciente paciente = pacienteRepository.findByDocumento(documento)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró un paciente con la cédula indicada."));

        List<Cita> citas;
        if (actor.getRol() == Rol.MEDICO_TERAPISTA) {
            Medico medico = medicoRepository.findByUsuario(actor)
                    .orElseThrow(() -> accesoDenegado(actor, "CONSULTAR_HISTORIAL", null));
            citas = citaRepository.findByPacienteIdAndMedicoIdOrderByFechaDescHoraDesc(paciente.getId(), medico.getId());
        } else if (actor.getRol() == Rol.AGENDADOR || actor.getRol() == Rol.ADMINISTRADOR) {
            citas = citaRepository.findByPacienteIdOrderByFechaDescHoraDesc(paciente.getId());
        } else {
            throw accesoDenegado(actor, "CONSULTAR_HISTORIAL", null);
        }
        securityAuditLogger.consultaExitosa(actor, "CONSULTAR_HISTORIAL");
        return citas.stream().map(HistorialCitaDto::from).toList();
    }

    @Transactional
    public CitaDto cambiarEstado(Usuario actor, Long citaId, EstadoCita nuevo) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        validarPropiedadDeCita(actor, cita, "CAMBIAR_ESTADO_CITA");
        if (actor.getRol() == Rol.PACIENTE && nuevo != EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("El paciente solo puede cancelar sus citas.");
        }
        cita.setEstado(nuevo);
        return CitaDto.from(citaRepository.save(cita));
    }

    @Transactional
    public CitaDto editar(Usuario actor, Long citaId, EditarCitaRequest request) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        validarPropiedadDeCita(actor, cita, "EDITAR_CITA");
        if (cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se pueden editar citas confirmadas. No es posible editar una cita cancelada o atendida.");
        }

        Long medicoId = request.getMedicoId() != null ? request.getMedicoId() : cita.getMedico().getId();
        LocalDate fecha = request.getFecha() != null ? request.getFecha() : cita.getFecha();
        LocalTime hora = request.getHora() != null ? request.getHora() : cita.getHora();

        Medico medico = medicoRepository.lockById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        validarFechaHabil(medico, fecha);
        List<LocalTime> franjas = generarFranjas(medico);
        if (!franjas.contains(hora)) {
            throw new ReglaNegocioException("La hora seleccionada no corresponde a una franja válida del profesional.");
        }
        List<Cita> citasBloqueadas = citaRepository.lockByMedicoAndFecha(medico.getId(), fecha);
        if (haySolapamiento(citasBloqueadas, hora, medico.getIntervaloMinutos(), cita.getId())) {
            throw new ConflictoReservaException(
                    "La franja horaria seleccionada acaba de ser ocupada por otro usuario. Seleccione otro horario.");
        }

        cita.setMedico(medico);
        cita.setFecha(fecha);
        cita.setHora(hora);
        cita.setDuracionMinutos(medico.getIntervaloMinutos());
        if (request.getModalidad() != null) {
            cita.setModalidad(request.getModalidad());
        }
        if (request.getMotivo() != null) {
            cita.setMotivo(request.getMotivo());
        }
        return CitaDto.from(citaRepository.save(cita));
    }

    public CitaDto porId(Usuario actor, Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        validarPropiedadDeCita(actor, cita, "CONSULTAR_CITA");
        return CitaDto.from(cita);
    }

    private void validarPropiedadDeCita(Usuario actor, Cita cita, String operacion) {
        if (actor.getRol() == Rol.AGENDADOR || actor.getRol() == Rol.ADMINISTRADOR) {
            return;
        }
        if (actor.getRol() == Rol.PACIENTE) {
            Paciente paciente = pacienteRepository.findByUsuario(actor)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
            if (cita.getPaciente().getId().equals(paciente.getId())) {
                return;
            }
        }
        if (actor.getRol() == Rol.MEDICO_TERAPISTA) {
            Medico medico = medicoRepository.findByUsuario(actor)
                    .orElseThrow(() -> accesoDenegado(actor, operacion, cita.getMedico().getId()));
            if (cita.getMedico().getId().equals(medico.getId())) {
                return;
            }
        }
        throw accesoDenegado(actor, operacion, cita.getMedico().getId());
    }

    private void validarAccesoAgenda(Usuario actor, Long medicoId, String operacion) {
        if (actor.getRol() == Rol.AGENDADOR || actor.getRol() == Rol.ADMINISTRADOR) {
            securityAuditLogger.consultaExitosa(actor, operacion);
            return;
        }
        if (actor.getRol() == Rol.MEDICO_TERAPISTA) {
            Medico medico = medicoRepository.findByUsuario(actor)
                    .orElseThrow(() -> accesoDenegado(actor, operacion, medicoId));
            if (medico.getId().equals(medicoId)) {
                securityAuditLogger.consultaExitosa(actor, operacion);
                return;
            }
        }
        throw accesoDenegado(actor, operacion, medicoId);
    }

    private org.springframework.security.access.AccessDeniedException accesoDenegado(
            Usuario actor, String operacion, Long medicoId) {
        securityAuditLogger.accesoDenegado(actor, operacion, medicoId);
        return new org.springframework.security.access.AccessDeniedException("No tiene permisos para esta información.");
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
        return haySolapamiento(citas, horaSolicitada, duracionSolicitada, null);
    }

    private boolean haySolapamiento(List<Cita> citas, LocalTime horaSolicitada, int duracionSolicitada, Long excluirCitaId) {
        LocalTime finSolicitado = horaSolicitada.plusMinutes(duracionSolicitada);
        return citas.stream()
                .filter(cita -> cita.getEstado() != EstadoCita.CANCELADA)
                .filter(cita -> excluirCitaId == null || !excluirCitaId.equals(cita.getId()))
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
