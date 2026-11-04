package com.bateriasapp.controller;

import com.bateriasapp.dto.FacturaRequest;
import com.bateriasapp.dto.FacturaResponse;
import com.bateriasapp.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Las facturas no se editan ni se borran: un error se corrige con una nota crédito (fuera de alcance). */
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public List<FacturaResponse> listar() {
        return facturaService.listar();
    }

    @GetMapping("/{id}")
    public FacturaResponse obtener(@PathVariable Integer id) {
        return facturaService.obtener(id);
    }

    @GetMapping("/pedido/{pedidoId}")
    public FacturaResponse obtenerPorPedido(@PathVariable Integer pedidoId) {
        return facturaService.obtenerPorPedido(pedidoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse generar(@Valid @RequestBody FacturaRequest request) {
        return facturaService.generar(request);
    }
}
