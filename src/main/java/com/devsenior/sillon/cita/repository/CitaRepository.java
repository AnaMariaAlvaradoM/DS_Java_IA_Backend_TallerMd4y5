package com.devsenior.sillon.cita.repository;

import com.devsenior.sillon.cita.model.Cita;
import com.devsenior.sillon.cita.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    @Query("SELECT c FROM Cita c WHERE c.estilista.id = :estilistaId " +
            "AND c.estado IN :estados " +
            "AND c.fechaHora >= :inicioDia AND c.fechaHora < :finDia")
    List<Cita> findCitasQueOcupanAgenda(
            @Param("estilistaId") Long estilistaId,
            @Param("estados") List<EstadoCita> estados,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);


    @Query("SELECT c FROM Cita c WHERE c.salon.id = :salonId AND c.fechaHora >= :inicioDia AND c.fechaHora < :finDia")
    List<Cita> findBySalonIdAndDia(
            @Param("salonId") Long salonId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);


    @Query("SELECT c FROM Cita c WHERE c.salon.id = :salonId AND c.estilista.id = :estilistaId AND c.fechaHora >= :inicioDia AND c.fechaHora < :finDia")
    List<Cita> findBySalonIdAndEstilistaIdAndDia(
            @Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);
}
