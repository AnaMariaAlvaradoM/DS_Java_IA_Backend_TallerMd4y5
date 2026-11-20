package com.devSenior.sillon.servicio.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.servicio.entidad.Servicio;

public interface ServicioRepositorio extends JpaRepository<Servicio, Long> {

	Optional<Servicio> findByIdAndSalon_Id(Long id, Long salonId);

	List<Servicio> findBySalon_IdOrderByIdAsc(Long salonId);

	List<Servicio> findBySalon_IdAndActivoOrderByIdAsc(Long salonId, boolean activo);

}
