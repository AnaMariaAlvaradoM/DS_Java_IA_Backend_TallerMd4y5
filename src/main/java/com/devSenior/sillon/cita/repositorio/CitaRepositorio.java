package com.devSenior.sillon.cita.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.cita.entidad.Cita;
import com.devSenior.sillon.cita.entidad.EstadoCita;

public interface CitaRepositorio extends JpaRepository<Cita, Long> {

	@EntityGraph(attributePaths = { "cliente", "estilista", "servicio" })
	Optional<Cita> findByIdAndSalon_Id(Long id, Long salonId);

	@EntityGraph(attributePaths = { "cliente", "estilista", "servicio" })
	List<Cita> findBySalon_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(Long salonId,
			LocalDateTime desde, LocalDateTime hasta);

	List<Cita> findByEstilista_IdAndSalon_IdAndEstadoIn(Long estilistaId, Long salonId, Collection<EstadoCita> estados);

}
