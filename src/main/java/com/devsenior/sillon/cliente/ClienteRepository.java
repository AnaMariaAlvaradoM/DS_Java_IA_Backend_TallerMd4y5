package com.devsenior.sillon.cliente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllBySalonId(Long salonId);

    Optional<Cliente> findBySalonIdAndTelefono(Long salonId, String telefono);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO clientes (salon_id, nombre, telefono)
            VALUES (:salonId, :nombre, :telefono)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void insertarSiNoExiste(@Param("salonId") Long salonId, @Param("nombre") String nombre,
            @Param("telefono") String telefono);

    // Lectura actual: puede ver la fila concurrente aunque la transaccion ya tenga un snapshot de MySQL.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cliente> findOneBySalonIdAndTelefono(Long salonId, String telefono);
}
