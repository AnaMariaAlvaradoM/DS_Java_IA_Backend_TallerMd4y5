package com.devsenior.sillon.cita;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Cita> findAllBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long salonId, Long estilistaId, Collection<EstadoCita> estados, LocalDateTime fin, LocalDateTime inicio);

    List<Cita> findAllBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findAllBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, Long estilistaId, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findAllBySalonIdAndEstilistaIdAndEstadoInAndInicioGreaterThanEqualAndInicioLessThan(
            Long salonId, Long estilistaId, Collection<EstadoCita> estados, LocalDateTime desde, LocalDateTime hasta);
}
