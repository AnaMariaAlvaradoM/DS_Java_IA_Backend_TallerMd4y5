package com.devsenior.sillon.cita;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    List<Cita> findBySalonIdAndInicioBetweenOrderByInicioAsc(
            Long salonId, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(
            Long salonId, Long estilistaId, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findBySalonIdAndEstilistaIdAndEstadoInAndInicioBetweenOrderByInicioAsc(
            Long salonId, Long estilistaId, Collection<EstadoCita> estados,
            LocalDateTime desde, LocalDateTime hasta);

    boolean existsBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long salonId, Long estilistaId, Collection<EstadoCita> estados,
            LocalDateTime finNueva, LocalDateTime inicioNueva);
}
