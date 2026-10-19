package com.devsenior.sillon.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);
    List<Servicio> findAllBySalonId(Long salonId);
    List<Servicio> findAllByIdInAndSalonId(Collection<Long> ids, Long salonId);
}
