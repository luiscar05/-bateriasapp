package com.bateriasapp.repository;

import com.bateriasapp.model.Domiciliario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomiciliarioRepository extends JpaRepository<Domiciliario, Integer> {
}