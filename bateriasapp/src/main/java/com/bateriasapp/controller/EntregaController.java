package com.bateriasapp.controller;

import com.bateriasapp.dto.EntregaActualizarRequest;
import com.bateriasapp.dto.EntregaEstadoRequest;
import com.bateriasapp.dto.EntregaRequest;
import com.bateriasapp.dto.EntregaResponse;
import com.bateriasapp.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @GetMapping
    public List<EntregaResponse> listar() {
        return entregaService.listar();
    }

    @GetMapping("/{id}")
    public EntregaResponse obtener(@PathVariable Integer id) {
        return entregaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntregaResponse crear(@Valid @RequestBody EntregaRequest request) {
        return entregaService.crear(request);
    }

    @PutMapping("/{id}")
    public EntregaResponse actualizar(@PathVariable Integer id,
                                      @Valid @RequestBody EntregaActualizarRequest request) {
        return entregaService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    public EntregaResponse cambiarEstado(@PathVariable Integer id,
                                         @Valid @RequestBody EntregaEstadoRequest request) {
        return entregaService.cambiarEstado(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        entregaService.eliminar(id);
    }
}
