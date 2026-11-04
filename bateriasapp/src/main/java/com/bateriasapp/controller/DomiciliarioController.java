package com.bateriasapp.controller;


import com.bateriasapp.model.Domiciliario;
import com.bateriasapp.repository.DomiciliarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/domiciliarios")

public class DomiciliarioController {

    private final DomiciliarioRepository domiciliarioRepository;

    public DomiciliarioController(DomiciliarioRepository domiciliarioRepository){this.domiciliarioRepository = domiciliarioRepository;}


    @GetMapping
    public List<Domiciliario> listarTodos(){
        return domiciliarioRepository.findAll();
    }

    @GetMapping("/{id}")
    public Domiciliario obtenerPorId(@PathVariable Integer id){
        return domiciliarioRepository.findById(id).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<Domiciliario> crearDomiciliario(@RequestBody Domiciliario dto){
        Domiciliario nuevoDomiciliario = domiciliarioRepository.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDomiciliario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id){
        domiciliarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Domiciliario> actualizarPorId(@PathVariable Integer id, @RequestBody Domiciliario dto){
        Domiciliario actualizarDomiciliario = domiciliarioRepository.findById(id).orElseThrow();
        actualizarDomiciliario.setNombre(dto.getNombre());
        actualizarDomiciliario.setTelefono(dto.getTelefono());
        Domiciliario domiciliarioActualizado = domiciliarioRepository.save(actualizarDomiciliario);
        return ResponseEntity.ok(domiciliarioActualizado);
    }


}
