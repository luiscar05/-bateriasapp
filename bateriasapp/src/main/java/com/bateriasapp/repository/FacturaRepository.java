package com.bateriasapp.repository;

import com.bateriasapp.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {

    boolean existsByPedidoId(Integer pedidoId);

    Optional<Factura> findByPedidoId(Integer pedidoId);
}
