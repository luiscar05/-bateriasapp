package com.bateriasapp.controller;


import com.bateriasapp.model.Producto;
import com.bateriasapp.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")

public class ProductoController {

    private final ProductoRepository productoRepository;

    public ProductoController(ProductoRepository productoRepository){
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public List<Producto> listarTodos(){
        return productoRepository.findAll();
    }

    @GetMapping("/{id}")
    public Producto obtenerPorId(@PathVariable Integer id){

        return productoRepository.findById(id).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<Producto> crearProducto(@RequestBody Producto dto){
        Producto nuevoProducto = productoRepository.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProducto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id){
        productoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizarPorId(@PathVariable Integer id, @RequestBody Producto dto){
        Producto actualizarProducto = productoRepository.findById(id).orElseThrow();
        actualizarProducto.setNombre(dto.getNombre());
        actualizarProducto.setTipo(dto.getTipo());
        actualizarProducto.setAmperaje(dto.getAmperaje());
        actualizarProducto.setCca(dto.getCca());
        actualizarProducto.setTipoCaja(dto.getTipoCaja());
        actualizarProducto.setPolaridad(dto.getPolaridad());
        actualizarProducto.setPrecioBase(dto.getPrecioBase());
        actualizarProducto.setCondicion(dto.getCondicion());
        actualizarProducto.setDisponible(dto.isDisponible());
        Producto productoActualizado = productoRepository.save(actualizarProducto);
        return ResponseEntity.ok(productoActualizado);

    }


}
