package com.devsenior.sillon.cita.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;

public interface CitaRepositorio extends JpaRepository<Cita, Long> {

	@Query("""
			SELECT c
			FROM Cita c
			JOIN FETCH c.salon
			JOIN FETCH c.cliente
			JOIN FETCH c.estilista
			JOIN FETCH c.servicio
			WHERE c.id = :id
			  AND c.salon.id = :salonId
			""")
	Optional<Cita> buscarPorIdYSalon(
			@Param("id") Long id,
			@Param("salonId") Long salonId);

	@Query("""
			SELECT c
			FROM Cita c
			JOIN FETCH c.cliente
			JOIN FETCH c.estilista
			JOIN FETCH c.servicio
			WHERE c.salon.id = :salonId
			  AND c.inicio >= :desde
			  AND c.inicio < :hasta
			ORDER BY c.inicio ASC, c.id ASC
			""")
	List<Cita> buscarPorSalonYRango(
			@Param("salonId") Long salonId,
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	@Query("""
			SELECT c
			FROM Cita c
			JOIN FETCH c.cliente
			JOIN FETCH c.estilista
			JOIN FETCH c.servicio
			WHERE c.salon.id = :salonId
			  AND c.estilista.id = :estilistaId
			  AND c.inicio >= :desde
			  AND c.inicio < :hasta
			ORDER BY c.inicio ASC, c.id ASC
			""")
	List<Cita> buscarPorSalonEstilistaYRango(
			@Param("salonId") Long salonId,
			@Param("estilistaId") Long estilistaId,
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	@Query("""
			SELECT c
			FROM Cita c
			WHERE c.salon.id = :salonId
			  AND c.estilista.id = :estilistaId
			  AND c.estado IN :estados
			  AND c.inicio < :hasta
			  AND c.fin > :desde
			ORDER BY c.inicio ASC, c.id ASC
			""")
	List<Cita> buscarCitasActivasEstilistaEnRango(
			@Param("salonId") Long salonId,
			@Param("estilistaId") Long estilistaId,
			@Param("estados") Collection<EstadoCita> estados,
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

}
