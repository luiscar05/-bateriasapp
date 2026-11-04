package com.bateriasapp.controller;

import com.bateriasapp.dto.BateriaUsadaEvaluacionRequest;
import com.bateriasapp.dto.BateriaUsadaRequest;
import com.bateriasapp.dto.BateriaUsadaResponse;
import com.bateriasapp.service.BateriaUsadaService;
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
@RequestMapping("/api/baterias-usadas")
public class BateriaUsadaController {

    private final BateriaUsadaService bateriaUsadaService;

    public BateriaUsadaController(BateriaUsadaService bateriaUsadaService) {
        this.bateriaUsadaService = bateriaUsadaService;
    }

    @GetMapping
    public List<BateriaUsadaResponse> listar() {
        return bateriaUsadaService.listar();
    }

    @GetMapping("/{id}")
    public BateriaUsadaResponse obtener(@PathVariable Integer id) {
        return bateriaUsadaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BateriaUsadaResponse registrar(@Valid @RequestBody BateriaUsadaRequest request) {
        return bateriaUsadaService.registrar(request);
    }

    @PutMapping("/{id}/evaluacion")
    public BateriaUsadaResponse evaluar(@PathVariable Integer id,
                                        @Valid @RequestBody BateriaUsadaEvaluacionRequest request) {
        return bateriaUsadaService.evaluar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        bateriaUsadaService.eliminar(id);
    }
}
