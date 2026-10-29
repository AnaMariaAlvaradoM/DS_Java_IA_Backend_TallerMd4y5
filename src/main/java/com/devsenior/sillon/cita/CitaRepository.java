package com.devsenior.sillon.cita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    List<Cita> findBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
            Long salonId, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
            Long salonId, Long estilistaId, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long salonId, Long estilistaId, List<EstadoCita> estados, LocalDateTime finNuevo,
            LocalDateTime inicioNuevo);
}
