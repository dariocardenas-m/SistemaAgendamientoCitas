package com.piedrazul.citas;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByPacienteIdOrderByFechaDescHoraDesc(Long pacienteId);

    List<Cita> findByPacienteIdAndMedicoIdOrderByFechaDescHoraDesc(Long pacienteId, Long medicoId);

    List<Cita> findByMedicoIdAndFechaOrderByHoraAsc(Long medicoId, LocalDate fecha);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cita c WHERE c.id = :id")
    Optional<Cita> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cita c WHERE c.medico.id = :medicoId AND c.fecha = :fecha")
    List<Cita> lockByMedicoAndFecha(@Param("medicoId") Long medicoId, @Param("fecha") LocalDate fecha);

    boolean existsByCodigo(String codigo);

    Optional<Cita> findByCodigo(String codigo);

    @Query("""
            SELECT c.medico.id, c.medico.nombreCompleto, MONTH(c.fecha), c.estado, COUNT(c)
            FROM Cita c
            WHERE c.fecha >= :desde AND c.fecha < :hasta
              AND (:medicoId IS NULL OR c.medico.id = :medicoId)
            GROUP BY c.medico.id, c.medico.nombreCompleto, MONTH(c.fecha), c.estado
            ORDER BY c.medico.nombreCompleto, MONTH(c.fecha)
            """)
    List<Object[]> reporteAnual(@Param("desde") LocalDate desde,
                                @Param("hasta") LocalDate hasta,
                                @Param("medicoId") Long medicoId);

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
