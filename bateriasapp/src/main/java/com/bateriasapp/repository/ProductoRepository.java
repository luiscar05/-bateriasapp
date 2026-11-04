package com.bateriasapp.repository;

import com.bateriasapp.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /**
     * Baterías vendibles con ficha técnica completa. El filtro "is not null" evita cargar filas
     * cuyo amperaje o cca sea NULL, porque Producto los mapea como int primitivo.
     */
    @Query("select p from Producto p where p.tipo = 'bateria' and p.disponible = true "
            + "and p.amperaje is not null and p.cca is not null")
    List<Producto> buscarBateriasDisponibles();
}
