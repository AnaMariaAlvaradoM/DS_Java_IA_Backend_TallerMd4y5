package com.devsenior.sillon.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudContactoRepositorio extends JpaRepository<SolicitudContacto, Long> {
    List<SolicitudContacto> findAllBySalon_IdOrderByFechaCreacionDescIdDesc(Long salonId);
    Optional<SolicitudContacto> findByIdAndSalon_Id(Long id, Long salonId);
}
