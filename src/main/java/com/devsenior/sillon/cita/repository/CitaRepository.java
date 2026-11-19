package com.devsenior.sillon.cita.repository;

import com.devsenior.sillon.cita.model.Cita;
import com.devsenior.sillon.cita.model.EstadoCita;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    /**
     * ¿El estilista tiene una cita que ocupa agenda y se cruza con [inicio, fin)?
     * Se cruzan si la existente empieza antes del fin nuevo y termina después del inicio nuevo.
     * Llamar con (estilistaId, estados, finNuevo, inicioNuevo).
     */
    boolean existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long estilistaId, Collection<EstadoCita> estados, LocalDateTime finNuevo, LocalDateTime inicioNuevo);

    /** Citas del estilista que ocupan agenda y tocan el rango [desde, hasta): sirve para la disponibilidad. */
    List<Cita> findByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long estilistaId, Collection<EstadoCita> estados, LocalDateTime hasta, LocalDateTime desde);

    List<Cita> findBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, Long estilistaId, LocalDateTime desde, LocalDateTime hasta);
}
