package com.devsenior.sillon.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Toda búsqueda se hace por id y salón a la vez: lo de otro salón no aparece (paso 3.1). */
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findAllBySalonId(Long salonId);

    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);

    List<Servicio> findAllBySalonIdAndIdIn(Long salonId, Collection<Long> ids);
}
