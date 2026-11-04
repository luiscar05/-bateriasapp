package com.bateriasapp.repository;

import com.bateriasapp.model.ConsecutivoFactura;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsecutivoFacturaRepository extends JpaRepository<ConsecutivoFactura, Integer> {

    /**
     * SELECT ... FOR UPDATE: dos facturas generadas al mismo tiempo no pueden leer el mismo
     * número; la segunda espera a que la primera termine su transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConsecutivoFactura c where c.id = :id")
    Optional<ConsecutivoFactura> buscarConBloqueo(@Param("id") Integer id);
}
