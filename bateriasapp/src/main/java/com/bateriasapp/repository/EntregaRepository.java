package com.bateriasapp.repository;

import com.bateriasapp.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaRepository extends JpaRepository<Entrega, Integer> {

    /** ¿El pedido ya tiene una entrega en un estado distinto al indicado (p. ej. una no fallida)? */
    boolean existsByPedidoIdAndEstadoNot(Integer pedidoId, String estado);
}
