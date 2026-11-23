
package com.devsenior.sillon.cita;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Buscar una cita dentro de su salón
    Optional<Cita> findByIdAndSalonId(
            Long id,
            Long salonId
    );

    // Consultar todas las citas del salón en un día
    List<Cita> findBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId,
            LocalDateTime inicioDia,
            LocalDateTime finDia
    );

    // Consultar las citas de un estilista en un día
    List<Cita> findBySalonIdAndEstilistaIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId,
            Long estilistaId,
            LocalDateTime inicioDia,
            LocalDateTime finDia
    );

    // Detectar citas que se cruzan con un horario
    boolean existsBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long salonId,
            Long estilistaId,
            Collection<EstadoCita> estados,
            LocalDateTime finPropuesto,
            LocalDateTime inicioPropuesto
    );
}
