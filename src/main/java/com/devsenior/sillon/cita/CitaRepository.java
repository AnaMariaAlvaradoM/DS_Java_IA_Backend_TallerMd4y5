package com.devsenior.sillon.cita;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cita> findOneByIdAndSalonId(Long id, Long salonId);

    @EntityGraph(attributePaths = {"cliente", "estilista", "servicio"})
    List<Cita> findAllBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, LocalDateTime desde, LocalDateTime hasta);

    @EntityGraph(attributePaths = {"cliente", "estilista", "servicio"})
    List<Cita> findAllBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, Long estilistaId, LocalDateTime desde, LocalDateTime hasta);

    boolean existsBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long salonId, Long estilistaId, Collection<EstadoCita> estados, LocalDateTime finSolicitado,
            LocalDateTime inicioSolicitado);
}
