package com.devsenior.sillon.cita.repositorio;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    boolean existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long estilistaId,
            Collection<EstadoCita> estados,
            LocalDateTime finNueva,
            LocalDateTime inicioNueva);

    List<Cita> findByEstilistaIdAndEstadoInAndInicioBetweenOrderByInicioAsc(
            Long estilistaId,
            Collection<EstadoCita> estados,
            LocalDateTime desde,
            LocalDateTime hasta);

    List<Cita> findBySalonIdAndInicioBetweenOrderByInicioAsc(
            Long salonId,
            LocalDateTime desde,
            LocalDateTime hasta);

    List<Cita> findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(
            Long salonId,
            Long estilistaId,
            LocalDateTime desde,
            LocalDateTime hasta);
}