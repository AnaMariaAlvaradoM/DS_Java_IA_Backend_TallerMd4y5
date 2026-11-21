package com.devsenior.sillon.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    List<Servicio> findAllBySalonIdOrderByIdAsc(Long salonId);
    List<Servicio> findAllBySalonIdAndActivoTrueOrderByNombreAsc(Long salonId);
    List<Servicio> findAllByIdInAndSalonId(Collection<Long> ids, Long salonId);
    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);
}
