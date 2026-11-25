package com.devsenior.sillon.servicio.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsenior.sillon.servicio.modelo.Servicio;

public interface ServicioRepositorio extends JpaRepository<Servicio, Long> {

	List<Servicio> findAllBySalonIdOrderByIdAsc(Long salonId);

	List<Servicio> findAllBySalonIdAndActivoTrueOrderByIdAsc(Long salonId);

	Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);

	List<Servicio> findAllByIdInAndSalonId(Collection<Long> ids, Long salonId);

}
