package com.devsenior.sillon.servicio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepositorio extends JpaRepository<Servicio, Long> {
    Optional<Servicio> findByIdAndSalon_Id(Long id, Long salonId);
    List<Servicio> findAllBySalon_IdOrderByIdAsc(Long salonId);
}
