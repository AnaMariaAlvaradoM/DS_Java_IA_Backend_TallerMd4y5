package com.devSenior.sillon.chat.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.chat.entidad.EstadoSolicitud;
import com.devSenior.sillon.chat.entidad.SolicitudContacto;

public interface SolicitudContactoRepositorio extends JpaRepository<SolicitudContacto, Long> {

	List<SolicitudContacto> findBySalon_IdOrderByFechaCreacionDescIdDesc(Long salonId);

	List<SolicitudContacto> findBySalon_IdAndEstadoOrderByFechaCreacionDescIdDesc(Long salonId, EstadoSolicitud estado);

	Optional<SolicitudContacto> findByIdAndSalon_Id(Long id, Long salonId);

}
