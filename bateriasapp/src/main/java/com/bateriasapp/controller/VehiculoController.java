package com.bateriasapp.controller;


import com.bateriasapp.model.Vehiculo;
import com.bateriasapp.repository.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoController(VehiculoRepository vehiculoRepository){this.vehiculoRepository = vehiculoRepository;}

    @GetMapping
    public List<Vehiculo> listarTodos(){
        return vehiculoRepository.findAll();
    }

    @GetMapping("/{id}")
    public Vehiculo obtenerPorId(@PathVariable Integer id){
        return vehiculoRepository.findById(id).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<Vehiculo> crearVehiculo(@RequestBody Vehiculo dto){
        Vehiculo nuevoVehiculo = vehiculoRepository.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoVehiculo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id){
        vehiculoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> actualizarPorId(@PathVariable Integer id, @RequestBody Vehiculo dto){
        Vehiculo actualizarVehiculo = vehiculoRepository.findById(id).orElseThrow();
        actualizarVehiculo.setMarca(dto.getMarca());
        actualizarVehiculo.setModelo(dto.getModelo());
        actualizarVehiculo.setAnio(dto.getAnio());
        actualizarVehiculo.setAmperajeRecomendado(dto.getAmperajeRecomendado());
        actualizarVehiculo.setCcaRecomendado(dto.getCcaRecomendado());
        Vehiculo vehiculoActualizado = vehiculoRepository.save(actualizarVehiculo);
        return ResponseEntity.ok(vehiculoActualizado);
    }

}
