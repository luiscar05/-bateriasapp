package com.bateriasapp.repository;

import com.bateriasapp.model.BateriaUsada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BateriaUsadaRepository extends JpaRepository<BateriaUsada, Integer> {

    boolean existsByPedidoId(Integer pedidoId);

    List<BateriaUsada> findByEstadoEvaluacionOrderByFechaRecepcionAsc(String estadoEvaluacion);
}
