package com.devsenior.sillon.servicio;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);

    List<Servicio> findBySalonId(Long salonId);
    List<Servicio> findAllByIdInAndSalonId(Set<Long> ids, Long salonId);

}
