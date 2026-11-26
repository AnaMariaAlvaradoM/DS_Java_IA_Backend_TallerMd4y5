package com.devsenior.sillon.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    /** Buscar siempre por id Y salón: si es de otro salón, no aparece (404). */
    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);

    List<Servicio> findAllBySalonIdOrderByIdAsc(Long salonId);

    List<Servicio> findAllBySalonIdAndActivoTrueOrderByIdAsc(Long salonId);

    List<Servicio> findAllByIdInAndSalonId(Collection<Long> ids, Long salonId);
}
