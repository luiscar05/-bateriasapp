package com.bateriasapp.controller;

import com.bateriasapp.model.Cliente;
import com.bateriasapp.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")

public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository){this.clienteRepository = clienteRepository;}

    @GetMapping
    public List<Cliente> listarTodos(){
        return clienteRepository.findAll();
    }

    @GetMapping("/{id}")
    public Cliente obtenerPorId(@PathVariable Integer id){
        return clienteRepository.findById(id).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<Cliente> crearCliente(@RequestBody Cliente dto){
        Cliente nuevoCliente = clienteRepository.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCliente);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id){
        clienteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarPorId(@PathVariable Integer id, @RequestBody Cliente dto){
        Cliente actualizarCliente = clienteRepository.findById(id).orElseThrow();
        actualizarCliente.setNombre(dto.getNombre());
        actualizarCliente.setCedula((dto.getCedula()));
        actualizarCliente.setTelefono(dto.getTelefono());
        actualizarCliente.setDireccion(dto.getDireccion());
        actualizarCliente.setCiudad(dto.getCiudad());
        Cliente clienteActualizado = clienteRepository.save(actualizarCliente);
        return ResponseEntity.ok(clienteActualizado);
    }


}
