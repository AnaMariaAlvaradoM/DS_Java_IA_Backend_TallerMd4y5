package com.devsenior.sillon.cita;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    /** ¿Hay una cita del estilista que ocupe agenda y se cruce con [inicio, fin)? */
    boolean existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(Long estilistaId,
            Collection<EstadoCita> estados, LocalDateTime fin, LocalDateTime inicio);

    /** Citas del salón en un día: [desde, hasta). */
    List<Cita> findAllBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(Long salonId,
            LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findAllBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, Long estilistaId, LocalDateTime desde, LocalDateTime hasta);

    /** Citas que ocupan agenda de un estilista en un día (para la disponibilidad). */
    List<Cita> findAllByEstilistaIdAndEstadoInAndInicioGreaterThanEqualAndInicioLessThan(Long estilistaId,
            Collection<EstadoCita> estados, LocalDateTime desde, LocalDateTime hasta);
}
