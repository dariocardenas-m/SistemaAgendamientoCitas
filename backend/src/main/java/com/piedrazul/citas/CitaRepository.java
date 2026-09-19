package com.piedrazul.citas;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByPacienteIdOrderByFechaDescHoraDesc(Long pacienteId);

    List<Cita> findByMedicoIdAndFechaOrderByHoraAsc(Long medicoId, LocalDate fecha);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cita c WHERE c.medico.id = :medicoId AND c.fecha = :fecha")
    List<Cita> lockByMedicoAndFecha(@Param("medicoId") Long medicoId, @Param("fecha") LocalDate fecha);

    boolean existsByCodigo(String codigo);

    Optional<Cita> findByCodigo(String codigo);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Cita c
            WHERE c.medico.id = :medicoId AND c.fecha = :fecha AND c.hora = :hora
              AND c.estado <> com.piedrazul.citas.EstadoCita.CANCELADA
            """)
    boolean existeFranjaOcupada(@Param("medicoId") Long medicoId,
                                @Param("fecha") LocalDate fecha,
                                @Param("hora") LocalTime hora);
}
