package com.devsenior.sillon.chat.repository;

import com.devsenior.sillon.chat.model.SolicitudContacto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    List<SolicitudContacto> findBySalonIdOrderByFechaCreacionDescIdDesc(Long salonId);

    Optional<SolicitudContacto> findByIdAndSalonId(Long id, Long salonId);
}
