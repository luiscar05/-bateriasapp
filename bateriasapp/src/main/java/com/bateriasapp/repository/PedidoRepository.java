package com.bateriasapp.repository;

import com.bateriasapp.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /** Cada fila: [metodoPago (String), cantidad (Long), suma de precioFinal (BigDecimal)]. */
    @Query("select p.metodoPago, count(p), sum(p.precioFinal) from Pedido p "
            + "where p.fechaCreacion >= :desde and p.fechaCreacion < :hastaExclusivo "
            + "and p.estado in :estados group by p.metodoPago")
    List<Object[]> resumirVentasPorMetodoPago(@Param("desde") LocalDateTime desde,
                                              @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
                                              @Param("estados") Collection<String> estados);

    /** Cada fila: [estado (String), cantidad (Long)]. */
    @Query("select p.estado, count(p) from Pedido p group by p.estado")
    List<Object[]> contarPorEstado();
}
