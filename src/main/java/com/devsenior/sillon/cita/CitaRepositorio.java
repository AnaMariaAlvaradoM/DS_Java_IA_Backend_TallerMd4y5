package com.devsenior.sillon.cita;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CitaRepositorio extends JpaRepository<Cita, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c where c.salon.id = :salonId and c.estilista.id = :estilistaId "
            + "and c.estado in :estados and c.inicio < :fin and c.fin > :inicio")
    List<Cita> buscarCrucesParaActualizar(@Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId, @Param("estados") List<EstadoCita> estados,
            @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("select c from Cita c where c.salon.id = :salonId and c.estilista.id = :estilistaId "
            + "and c.estado in :estados and c.inicio < :hasta and c.fin > :desde")
    List<Cita> buscarOcupadas(@Param("salonId") Long salonId, @Param("estilistaId") Long estilistaId,
            @Param("estados") List<EstadoCita> estados, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    List<Cita> findAllBySalon_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, LocalDateTime desde, LocalDateTime hasta);
    List<Cita> findAllBySalon_IdAndEstilista_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, Long estilistaId, LocalDateTime desde, LocalDateTime hasta);
    Optional<Cita> findByIdAndSalon_Id(Long id, Long salonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c where c.id = :id and c.salon.id = :salonId")
    Optional<Cita> buscarParaActualizar(@Param("id") Long id, @Param("salonId") Long salonId);
}
