package com.devsenior.sillon.cita;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    boolean existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long estilistaId,
            Collection<EstadoCita> estados,
            LocalDateTime fin,
            LocalDateTime inicio
    );

    List<Cita> findByEstilistaIdAndEstadoInAndInicioGreaterThanEqualAndFinLessThanEqual(
            Long estilistaId,
            Collection<EstadoCita> estados,
            LocalDateTime rangoInicio,
            LocalDateTime rangoFin
    );

    List<Cita> findBySalonIdAndInicioBetweenOrderByInicioAsc(
            Long salonId,
            LocalDateTime inicioDia,
            LocalDateTime finDia
    );

    List<Cita> findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(
            Long salonId,
            Long estilistaId,
            LocalDateTime inicioDia,
            LocalDateTime finDia
    );

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    List<Cita> findByEstilistaIdAndEstadoInAndInicioBetween(
            Long estilistaId,
            Collection<EstadoCita> estados,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}
