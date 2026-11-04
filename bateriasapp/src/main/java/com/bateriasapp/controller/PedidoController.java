package com.bateriasapp.controller;

import com.bateriasapp.dto.EstadoRequest;
import com.bateriasapp.dto.PedidoActualizarRequest;
import com.bateriasapp.dto.PedidoRequest;
import com.bateriasapp.dto.PedidoResponse;
import com.bateriasapp.service.PedidoService;
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

/** El controller solo recibe, valida (@Valid), delega al servicio y responde. */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidoService.listar();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Integer id) {
        return pedidoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest request) {
        return pedidoService.crear(request);
    }

    @PutMapping("/{id}")
    public PedidoResponse actualizar(@PathVariable Integer id,
                                     @Valid @RequestBody PedidoActualizarRequest request) {
        return pedidoService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Integer id,
                                        @Valid @RequestBody EstadoRequest request) {
        return pedidoService.cambiarEstado(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        pedidoService.eliminar(id);
    }
}
