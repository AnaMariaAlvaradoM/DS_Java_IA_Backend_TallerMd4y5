package com.devsenior.sillon.servicio.repository;

import com.devsenior.sillon.servicio.model.Servicio;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Toda búsqueda filtra por salón: un servicio de otro salón "no existe" (404). */
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findBySalonIdOrderByNombreAsc(Long salonId);

    List<Servicio> findBySalonIdAndActivoTrueOrderByNombreAsc(Long salonId);

    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);

    List<Servicio> findByIdInAndSalonId(Collection<Long> ids, Long salonId);
}
